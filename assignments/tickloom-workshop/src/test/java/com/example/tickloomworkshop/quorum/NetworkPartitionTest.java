package com.example.tickloomworkshop.quorum;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Template for a TickLoom-style network partition test.
 *
 * Refer to tickloom's NetworkPartitionTest for full wiring using the Cluster testkit and SimulatedNetwork.
 */
public class NetworkPartitionTest {

    @Test
    @Disabled("Template: wire up tickloom Cluster and SimulatedNetwork. See tickloomexamples.")
    void write_then_partition_then_read_behavior() {
        // Arrange: start 3 replicas, perform a quorum write
        // Act: partition one follower; perform reads targeting different sides
        // Assert: demonstrate potential inconsistency without repair; then enable repair and verify
    }
}














