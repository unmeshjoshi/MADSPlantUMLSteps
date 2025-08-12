# Assignment Solutions and Solution Outlines

This document provides solution outlines and reference answers for the workshop assignments in `quorums-consensus-workshop.yaml`. For implementation labs, the steps target the deterministic TickLoom framework (see [TickLoom](https://github.com/unmeshjoshi/tickloom)).

---

## 1) Failure Probability and Capacity (Foundations)

- Disk failures
  - Per-disk daily probability p = 1/1000 = 0.001
  - Expected failures/day for 10,000 disks = 10,000 × 0.001 = 10
  - P(at least one failure/day) = 1 − (1 − p)^(10000) ≈ 1 − e^(−10) ≈ 0.9999546

- Little’s Law
  - Use L = λ T (T = total time in system)
  - λ = 5000 req/s, T = 0.2 s → L = 1000 concurrent in-flight users
  - Valid only if system is stable at that λ (λ < capacity)

- Network cap (1 Gbps ≈ 125 MB/s)
  - 1 KB → ≈ 125,000 RPS (upper bound)
  - 10 KB → ≈ 12,500 RPS

- Disk sequential read
  - 1 TB at 500 MB/s → 1,000,000 MB / 500 = 2000 s ≈ 33.3 minutes

---

## 2) Little’s Law and Utilization (Dedicated Assignment)

- L = λ T → with λ = 5000/s, T = 200 ms → L = 1000
- T = Wq + S → with S = 50 ms, T = 180 ms → Wq = 130 ms
- μ = 1/S → S = 50 ms ⇒ μ = 20/s per server
- Utilization ρ = λ / (m μ), stability requires λ < m μ
- Example: μ = 1000/s per server, m = 3 → capacity 3000/s
  - λ at ρ = 0.8 → 2400/s; at ρ = 0.95 → 2850/s (expect latency knee near here)
- Notation tip: prefer T for total time (sojourn): L = λ T, T = Wq + S

---

## 3) Quorum Math and Overlap (Foundations)

- Majority quorum q(N) = ⌊N/2⌋ + 1; overlap ≥ 2q − N (≥ 1 for majorities)
- Examples: N=3→q=2, N=5→q=3, N=7→q=4, N=9→q=5
- Explicit intersection (N=5): {1,2,3} ∩ {3,4,5} = {3}
- Unsafe if q too small (N=5, q=2): {1,2} ∩ {3,4} = ∅

---

## 4) Quorum KV with Read/Write Quorums (TickLoom)

Goal: 3-node KV with W=2, R=2; show inconsistency; add repairs; show clock-skew pitfall.

- State and messages
  - ValueVersion = (value, version/timestamp)
  - PutReq/PutAck, GetReq/GetResp, RepairReq/RepairAck

- Write path
  1) Leader writes locally and sends PutReq(value, version) to peers
  2) Wait for W=2 acks (including self), then success

- Read path
  - Issue reads to all; wait for R=2; choose highest version

- Incomplete write simulation
  - Drop one PutReq; quorum still succeeds; one replica stale
  - Reads from different replicas can diverge

- Sync vs Async read-repair
  - Sync: repair stale responders before replying (higher latency)
  - Async: send repairs, reply immediately (temporary staleness)

- Clock skew pitfall
  - Use per-node physical timestamps; skew one node forward
  - Show overwrite anomaly; fix with logical clocks or leader-assigned versions

- Tests (TickLoom)
  - Deterministically drop/delay messages; assert values and repair effects

---

## 5) Minimal Consensus with Recovery (TickLoom)

Goal: Minimal Paxos-style single-slot safety with recovery.

- Data: generation (ballot), accepted (gen, value)
- Protocol
  1) Prepare(g) → Promise (may include prior accepted)
  2) Choose highest-generation prior if any; else propose own
  3) Accept(g, value) → Accepted (majority)
  4) Commit/expose after majority Accepted
- Recovery: new leader runs Prepare, adopts highest prior, re-proposes
- Safety: if any value was committed, no later different value can be committed
- Deterministic tests: script loss/crash; assert single decided value across replicas

---

## 6) Replicated Log and High-Water Mark (TickLoom)

Goal: Per-slot consensus with in-order execution via HWM.

- State: log[slot] = (acceptedGen, value, committed), highWaterMark (HWM)
- Execute only up to HWM (largest index with all slots ≤ i committed)
- Tests
  - Commit slots 1 and 3, leave 2 pending → HWM = 1; slot 3 must not execute
  - Commit slot 2 → HWM advances to 3; execute 2 then 3 in order
  - Failures during accept/commit → no lost or reordered execution
- Metrics (optional): commit latency distribution vs delays/loss

---

## 7) Instrumentation and SLOs (TickLoom)

Goal: Measure quorum/consensus health and propose SLOs.

- Instrument: quorum success rate, commit latency P50/P95/P99, read staleness window
- Experiments: sweep delay/loss; capture metrics across ticks; identify safe operating point (< ~80% utilization)
- Output: brief ops report with tables/graphs, SLO targets

---

## References

- TickLoom framework: https://github.com/unmeshjoshi/tickloom
- Queueing formulas: L = λ T; T = Wq + S; μ = 1/S; ρ = λ/(m μ); stability λ < m μ
