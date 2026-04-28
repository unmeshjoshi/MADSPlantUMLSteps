# Deterministic Simulation Implementation Checklist


This checklist is a language-neutral guide for building a deterministic-simulation-friendly
implementation.


The goal is:


- make process logic explicit
- make network progress explicit
- make timeouts explicit
- drive everything from a deterministic test or main loop
- record client-visible history for correctness checking
- make faults replayable by seed or by an explicit schedule


## Core Design Contract


- [ ] No core correctness logic depends directly on wall-clock time.
- [ ] No hidden background thread mutates authoritative process state.
- [ ] Messages are delivered only when the driver advances the network.
- [ ] Local timeout/retry logic advances only when the driver advances a process tick or controlled clock.
- [ ] Client-visible behavior is recorded as history, not inferred from internal state.
- [ ] The same seed, workload, and fault schedule produce the same history.


## 1. Model The Process


- [ ] Create a `Process` abstraction that owns local state.
- [ ] Give each process a stable `ProcessId`.
- [ ] Give the process a handler per `MessageType`, or a single `receive(message, context)` entrypoint that dispatches by message type.
- [ ] Expose a `tick()` method on the process.
- [ ] Pass a process context into handlers and ticks.
- [ ] Let the process emit outbound messages only through the context or message bus.
- [ ] Include the current logical tick or controlled clock in the context.
- [ ] On `tick()`, progress any local time-based logic that exists, such as retries, leases, waits, or timeouts.
- [ ] If the implementation later adds timeout-tracking components, advance them from `tick()` rather than from wall-clock callbacks.


### Process responsibilities


- handle messages
- update local state
- emit outbound messages
- advance local timeout/waiting logic when the implementation has it
- avoid direct wall-clock reads in core correctness paths


### Process acceptance checks


- [ ] A process can handle a request and emit a response without real threads or sleeps.
- [ ] A process can record a completion using the explicit logical tick.
- [ ] A process with no time-based work has a harmless no-op `tick()`.


## 2. Model Messages


- [ ] Create a stable `Message` type.
- [ ] Include:
 - message id
 - source process id
 - destination process id
 - typed payload or message type plus body
- [ ] Use deterministic message id generation.
- [ ] Keep protocol payloads separate from the transport/simulation machinery where practical.


### Message responsibilities


- identify sender and receiver
- identify the operation or protocol step
- carry request ids when client-visible operations need to be correlated
- provide enough type information for handlers and fault rules


## 3. Model The Message Bus


- [ ] Create a `MessageBus` that keeps a `Map<ProcessId, Process>`.
- [ ] Make the `MessageBus` implement `MessageDispatcher`.
- [ ] Register processes with the `MessageBus`.
- [ ] When the network dispatches a message, route it to the target process with `receive(...)`.
- [ ] Let the bus send process-emitted messages into the network.
- [ ] Handle self-messages without going through the network when appropriate.
- [ ] Keep an explicit outbox for messages emitted while handling a message or ticking a process.
- [ ] Flush the outbox after process ticks and after network delivery.
- [ ] Preserve deterministic process tick order, for example by sorting process ids or using an ordered map.


### MessageBus responsibilities


- register processes
- send messages into the network
- route delivered messages to the correct process
- invoke process handlers
- manage emitted messages without hidden background progress


### MessageBus acceptance checks


- [ ] A response emitted by a handler is eventually sent through the network path.
- [ ] Self-messages are handled deterministically.
- [ ] Unknown destination behavior is explicit, either dropped, failed, or surfaced as an error/event.


## 4. Model The Network


- [ ] Create a `Network` abstraction for real or simulated transport.
- [ ] When `send(...)` is called, buffer outgoing messages.
- [ ] On `tick()`, progress sends and receives.
- [ ] Dispatch delivered messages to the registered `MessageDispatcher`.
- [ ] Keep network progress externally driven by the driver.


### Real network responsibilities


- buffer outbound messages
- progress non-blocking I/O on `tick()`
- receive inbound messages
- dispatch delivered messages
- keep mutation of authoritative process state outside I/O threads


## 5. Model A Simulated Network


- [ ] Create a `SimulatedNetwork`.
- [ ] Buffer sent messages instead of using real sockets.
- [ ] Deliver messages only when `tick()` is called.
- [ ] Track a logical network tick.
- [ ] Support message delay.
- [ ] Support message drop.
- [ ] Support network partitions.
- [ ] Support healing partitions.
- [ ] Optionally support replay or message-type-specific delay/drop rules.
- [ ] Preserve deterministic delivery ordering for messages due on the same tick.


### SimulatedNetwork responsibilities


- queue messages
- control delivery timing
- inject faults deterministically
- deliver messages only when the driver advances it


### SimulatedNetwork acceptance checks


- [ ] A delayed message is not delivered before its scheduled tick.
- [ ] A dropped message is never delivered.
- [ ] A partition prevents delivery between the partitioned endpoints.
- [ ] Healing a partition allows later messages to be delivered.
- [ ] Re-running the same scenario produces the same delivered message order.


## 6. Model The Cluster


- [ ] Create a `Cluster` abstraction that wires together:
 - processes
 - message bus
 - network
 - clocks or logical ticks
 - storage, when needed
- [ ] Expose helpers for:
 - partitions
 - healing
 - delay injection
 - packet loss
 - time control
- [ ] Provide a way to inject external/client messages into the cluster.
- [ ] Keep cluster-level tick ordering explicit.


### Recommended cluster tick order


1. Set the bus/process context to the current logical tick.
2. Tick processes to advance local time-based work.
3. Flush process outbox to the network.
4. Tick the network to deliver due messages.
5. Flush any messages emitted by receive handlers.


This ordering is not mandatory, but it must be chosen once, documented, and kept stable.


## 7. Model The Driver


- [ ] Create a driver program or test harness that explicitly advances the system.
- [ ] Call process ticks for local time-based progress when any exists.
- [ ] Call network ticks for message delivery progress.
- [ ] Keep the loop explicit and deterministic.
- [ ] Avoid sleeps as a correctness mechanism.


### Driver responsibilities


- decide when components advance
- avoid hidden background progress
- keep execution replayable
- define the observable order of workload, faults, process ticks, and network ticks


## 8. Keep Core Mutation Single-Threaded


- [ ] Keep core state changes inside one main execution thread per process or one deterministic driver thread.
- [ ] Allow async I/O and background work around the edges if needed.
- [ ] Do not let background threads race on authoritative process state.
- [ ] If using channels, queues, futures, or actors, drain them only at deterministic driver boundaries.
- [ ] Ensure all network and storage operations (send, read, flush) are strictly non-blocking.
- [ ] Never use `Thread.sleep()`, blocking locks, or blocking `Future.get()` inside the core process logic.
- [ ] Represent I/O completions as events (using callbacks, continuations, or `TickCompletableFuture`).
- [ ] Push real-world non-deterministic I/O (like NIO selectors or background disk writes) to the absolute edges of the architecture.
- [ ] Drain I/O completion events back into the process only at deterministic boundaries (the tick).


### Rule of thumb


Keep state mutation single-threaded. Push concurrency to the edges.


## 9. Make Time Explicit


- [ ] Do not let core correctness logic depend directly on wall-clock time.
- [ ] Add a `Timeout` abstraction when the protocol needs timeouts.
- [ ] Let each `Timeout` own its internal tick counter.
- [ ] Require `Timeout.start()` before it can fire.
- [ ] Advance timeouts only through explicit `tick()` calls.
- [ ] Expose `fired()`, `reset()`, `stop()`, and remaining-tick inspection.
- [ ] Use process ticks or controlled clocks to advance timeout state once timeout-tracking exists.
- [ ] Pass the current logical tick into handlers that record history or make timeout decisions.
- [ ] Make retries and timeout boundaries deterministic in tests.


### Timeout acceptance checks


- [ ] A timeout with duration `N` fires after exactly `N` ticks after `start()`.
- [ ] A stopped timeout does not fire.
- [ ] `reset()` restarts the timeout from the current internal tick.
- [ ] Remaining ticks reach zero once the timeout has fired.


## 10. Create A Hello World Example


- [ ] Create a minimal process to demonstrate the model end-to-end.
- [ ] Add one request message and one response message.
- [ ] Register processes through the `MessageBus`.
- [ ] Show that process ticks and network ticks are enough to drive the interaction.
- [ ] Record one invocation and one completion in history.
- [ ] Use this as the first deterministic demo before moving to quorum or replication.


### Hello World goals


- validate wiring between process, message bus, network, cluster, and driver
- provide a simple deterministic example that is easy to explain
- serve as the first integration test for the architecture


### Hello World acceptance checks


- [ ] The demo prints a readable history.
- [ ] The demo prints a checker result.
- [ ] The expected request/response completes without wall-clock sleeps.


## 11. Add A Generic SimulationRunner


- [ ] Create a generic `SimulationRunner`.
- [ ] Let it own or coordinate:
 - cluster creation
 - setup hooks
 - the driver loop
 - seeded randomness or explicit fault schedule
 - history collection
- [ ] Let it run the simulation for a chosen number of ticks.
- [ ] Add hook methods for implementation-specific workloads and fault injection.


### SimulationRunner responsibilities


- drive the cluster
- inject seeded randomness or replay explicit schedules
- run the tick loop
- return client-visible history


### Useful hooks


- `setup(cluster, history)`
- `before_tick(tick, cluster, history)`
- `after_tick(tick, cluster, history)`


## 12. Add Implementation-Specific Scenario Runners


- [ ] Create implementation-specific runners on top of `SimulationRunner`.
- [ ] Example: `QuorumKVScenarioRunner`.
- [ ] Define request generation logic in the scenario runner.
- [ ] Keep the generic driver reusable across implementations.
- [ ] Keep protocol-specific payloads and operations out of the generic simulation core where practical.


## 13. Add Client Request Issuers


- [ ] Create a `RequestIssuer` or `SingleRequestIssuer`.
- [ ] Let it issue client requests into the cluster.
- [ ] Keep request issuance deterministic under a seed.
- [ ] Track pending requests when the scenario needs to wait for completion or report timeouts.
- [ ] Correlate requests and responses with request ids.


### RequestIssuer responsibilities


- generate requests
- invoke client operations
- track pending requests when needed by the scenario
- connect client behavior to history recording


## 14. Add A RequestWaitingList


- [ ] Create a generic `RequestWaitingList<Key, Response>`.
- [ ] Store pending requests by correlation id, request id, or protocol-specific key.
- [ ] Store a callback per pending request.
- [ ] Store a per-request `Timeout` with the configured expiration duration.
- [ ] On `add(key, callback)`, create and start a timeout for that key.
- [ ] On `handle_response(key, response)`, remove the pending request and invoke the success callback.
- [ ] On `handle_response_from(key, response, from_node)`, include source process information in the callback.
- [ ] On `handle_error(key, error)`, remove the pending request and invoke the error callback.
- [ ] On `tick()`, tick all request timeouts, collect expired keys, remove them, and invoke timeout errors.
- [ ] On `clear()`, fail all pending requests and remove them.
- [ ] Integrate the waiting list into process/client tick flow before protocol-specific `on_tick` work when using a base process type.


### RequestWaitingList responsibilities


- centralize pending request tracking
- centralize tick-based timeout handling
- correlate responses to callbacks
- remove completed, failed, expired, and cancelled requests


### RequestWaitingList acceptance checks


- [ ] Adding a request increases the pending count.
- [ ] Handling a response removes the request and invokes the response callback.
- [ ] Handling an error removes the request and invokes the error callback.
- [ ] Ticking past expiration removes the request and invokes a timeout error.
- [ ] A late response for an expired or unknown request is ignored.
- [ ] Clearing the list invokes cancellation errors for all pending requests.


## 15. Record Client-Visible History


- [ ] Record invocations.
- [ ] Record successful completions.
- [ ] Record failures/timeouts/info events.
- [ ] Record fault events or enough metadata to reconstruct the run.
- [ ] Keep the history in a format suitable for correctness checking.
- [ ] Preserve process/client identity in the history.
- [ ] Preserve request ids or operation ids for correlation.
- [ ] Include logical ticks when useful for debugging and replay.


### History responsibilities


- capture what clients asked
- capture what clients observed
- support correctness checkers
- support demo/debug output


## 16. Add A QuorumKVReplica


- [ ] Create quorum-specific client messages:
 - client get request
 - client get response
 - client set request
 - client set response
- [ ] Create quorum-specific internal replica messages:
 - internal get request
 - internal get response
 - internal set request
 - internal set response
- [ ] Add a `VersionedValue` containing value bytes and a logical timestamp.
- [ ] Give each replica local key-value storage from key bytes to `VersionedValue`.
- [ ] On client `set`, broadcast internal set requests to all replicas, including self.
- [ ] On client `get`, broadcast internal get requests to all replicas, including self.
- [ ] Register each internal request in `RequestWaitingList` with a correlation id.
- [ ] Collect internal responses through a quorum callback or equivalent pending quorum state.
- [ ] Use majority quorum: `total_replicas / 2 + 1`.
- [ ] Complete `set` successfully once a majority of internal set responses are successful.
- [ ] Complete `get` successfully once a majority of internal get responses contain values.
- [ ] For `get`, return the value with the highest timestamp among successful quorum responses.
- [ ] Fail client requests when timeout/error handling proves quorum cannot be reached.
- [ ] Route late internal responses through `RequestWaitingList`, where unknown/expired keys are ignored.
- [ ] On internal `set`, write only if the stored timestamp is older than the incoming timestamp.
- [ ] On internal `set`, return success when the existing value is already at an equal or newer timestamp.
- [ ] Integrate `RequestWaitingList.tick()` into replica `tick()`.


### QuorumKVReplica responsibilities


- coordinate client get/set operations
- broadcast internal operations to replicas
- track per-operation quorum state
- apply timestamp-based last-writer-wins storage updates
- return client-visible responses only after quorum success or failure


### QuorumKVReplica acceptance checks


- [ ] A set request succeeds when a majority of replicas acknowledge it.
- [ ] A get request returns the latest timestamped value from a quorum.
- [ ] A get for a missing key returns a not-found response when quorum success is impossible.
- [ ] A set request fails when partitions/timeouts prevent quorum.
- [ ] An older internal set does not overwrite a newer stored value.
- [ ] A single-node cluster works with quorum size one.
- [ ] The behavior is deterministic under explicit process and network ticks.


## 17. Add Seeded Fault Injection


- [ ] Use a seeded pseudorandom generator for fault selection.
- [ ] Record the fault schedule.
- [ ] Ensure the same seed produces the same fault schedule.
- [ ] Combine the seeded fault schedule with deterministic execution.
- [ ] Support an explicit fault schedule for exact replay.
- [ ] Start with delay/drop/partition/heal before adding richer faults.


### Fault injection responsibilities


- explore many scenarios
- remain replayable
- support delay/drop/partition/heal workflows
- make presentation/demo runs stable


### Fault injection acceptance checks


- [ ] Two schedules generated with the same seed are identical.
- [ ] Two schedules generated with different seeds can differ.
- [ ] A manually supplied schedule is applied in tick order.
- [ ] Applied faults are visible in history or demo output.


## 18. Add Correctness Checkers


- [ ] Feed recorded histories into correctness checkers.
- [ ] Start with simple invariants, such as every invocation completes or times out.
- [ ] Add linearizability if the implementation exposes a replicated object or key-value API.
- [ ] Add scenario-specific or model-specific checkers as needed.
- [ ] Keep checker input decoupled from internal process state.


## 19. Add Demo-Friendly Output


- [ ] Print fault schedule in a readable form.
- [ ] Print history in a readable form.
- [ ] Print checker results.
- [ ] Keep one stable seed for presentation/demo use.
- [ ] Add at least one scenario that is easy to explain live.


## Suggested Build Order


1. Message and process ids
2. Process abstraction
3. MessageBus and dispatcher
4. Network abstraction
5. SimulatedNetwork
6. Cluster
7. Driver loop
8. Hello World example
9. History recording
10. SimulationRunner
11. RequestIssuer
12. Timeout
13. RequestWaitingList
14. Implementation-specific protocol, such as quorum key-value
15. Seeded fault injection
16. Correctness checker
17. Demo/presentation scenario


## Minimum Working Slice


A first implementation is useful when it can:


- run a hello-world request/response scenario
- deliver messages only through explicit ticks
- delay messages deterministically
- drop or partition messages deterministically
- record invocation and completion history
- expire pending requests through tick-driven timeouts
- complete quorum key-value get/set operations
- run a simple checker over the history
- replay the same seeded schedule


## Language Mapping Notes


- Java: interfaces for `Process`, `Network`, and `MessageDispatcher`; classes for `Cluster`, `MessageBus`, and runners.
- Rust: traits for `Process`, `Network`, and `Scenario`; explicit context objects work well for outbound messages and logical time.
- Go: interfaces for process/network boundaries; structs for cluster, bus, and simulation runner.
- C++: abstract base classes or templates for process/network boundaries; prefer deterministic containers for process order.


The architecture is portable because the important choices are about ownership, message flow, time,
and replay, not about any one language feature.


## TickCompletableFuture (from `~/work/tickloom`)

The `TickCompletableFuture` class provides a single, unified non-blocking callback mechanism designed for single-threaded event loops. It allows chaining operations and handling completions seamlessly.

### Why not use CompletableFuture in Deterministic Simulation Testing (DST)?

In Deterministic Simulation Testing, it is critical that the entire execution is completely predictable and reproducible. Standard Java `CompletableFuture` methods often default to running asynchronous callbacks on multi-threaded thread pools (like `ForkJoinPool.commonPool()`). This introduces non-determinism through OS-level thread scheduling, uncontrollable concurrent execution, and potential data races.

By contrast, `TickCompletableFuture` is designed explicitly to run entirely within the single-threaded deterministic event loop (the "tick" loop). All state transitions and callbacks are executed sequentially and predictably without spawning background threads. This ensures that the simulation has absolute, deterministic control over the order of execution, allowing tests to perfectly reproduce behavior on every run.

### Key Methods

- **`thenApply(Function<T, U> fn)`**: Transforms the result of the future using the provided function and returns a new `TickCompletableFuture<U>`. Exceptions in the function or the original future propagate to the returned future.
- **`thenCompose(Function<T, TickCompletableFuture<U>> fn)`**: Chains another asynchronous operation that returns a `TickCompletableFuture`. This flattens the nested futures into a single `TickCompletableFuture<U>`.
- **`whenComplete(BiConsumer<T, Throwable> callback)`**: Registers a callback to be invoked when the future resolves, handling both success and failure cases. Returns a new future for method chaining.
- **`whenComplete(Continuation<T> c)`**: Convenience method to integrate with a `Continuation`, resuming it with the result or error.
- **`complete(T result)`**: Resolves the future successfully with the given result. Invokes any registered callback.
- **`fail(Throwable exception)`**: Fails the future with the given exception. Invokes any registered callback.
- **State Checking**:
  - `isPending()`: Checks if the future is still unresolved.
  - `isCompleted()`: Checks if the future resolved successfully.
  - `isFailed()`: Checks if the future failed with an exception.
- **Result Access**:
  - `getResult()`: Retrieves the successfully completed result (throws if not successfully completed).
  - `getException()`: Retrieves the failure exception (throws if not failed).


## Final Architecture Goal


Make process logic, messaging, time, storage, and fault injection explicit.
Then drive them through a deterministic loop that can be replayed by seed and checked through
client-visible history.

