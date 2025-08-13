package com.example.tickloomworkshop.quorum;

// Template based on TickLoom's Process/Replica patterns.
// Refer to QuorumReplica and related classes in tickloom for full implementation patterns.

import java.util.HashMap;
import java.util.Map;

/**
 * QuorumReplica: skeleton for a replica participating in R/W quorums.
 *
 * TODOs:
 * - Integrate with TickLoom's Process base and MessageBus.
 * - Implement write quorum (W=2 of 3) and read quorum (R=2 of 3).
 * - Add repair (sync/async) paths.
 * - Instrument quorum success and latency (ticks).
 */
public class QuorumReplica {

    public static final class VersionedValue {
        public final String value;
        public final long version;
        public VersionedValue(String value, long version) {
            this.value = value;
            this.version = version;
        }
    }

    private final String id;
    private final Map<String, VersionedValue> kv = new HashMap<>();
    private long logicalClock = 0L;

    public QuorumReplica(String id) {
        this.id = id;
    }

    // Local helpers (hook up to TickLoom message handlers in practice)
    public VersionedValue localGet(String key) { return kv.get(key); }
    public VersionedValue localPut(String key, String value) {
        long v = ++logicalClock; // Replace with leader-assigned or Lamport clock
        VersionedValue vv = new VersionedValue(value, v);
        kv.put(key, vv);
        return vv;
    }
}




