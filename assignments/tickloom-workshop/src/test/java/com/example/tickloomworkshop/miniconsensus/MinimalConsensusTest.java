package com.example.tickloomworkshop.miniconsensus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MinimalConsensusTest {

    @Test
    void promise_then_accept_commits_value() {
        SingleSlotConsensusReplica r = new SingleSlotConsensusReplica();
        assertTrue(r.onPrepare(1));
        assertTrue(r.onAccept(1, "V"));
        r.commit();
        assertTrue(r.isCommitted());
        assertEquals("V", r.getAcceptedValue());
    }
}





