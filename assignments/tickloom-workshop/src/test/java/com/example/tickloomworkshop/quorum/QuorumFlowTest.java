package com.example.tickloomworkshop.quorum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QuorumFlowTest {

    @Test
    void local_put_get_smoke() {
        QuorumReplica r = new QuorumReplica("r1");
        r.localPut("K", "V1");
        var vv = r.localGet("K");
        assertNotNull(vv);
        assertEquals("V1", vv.value);
        assertTrue(vv.version > 0);
    }
}



