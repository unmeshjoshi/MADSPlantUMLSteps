package com.example.tickloomworkshop.replog;

import com.example.tickloomworkshop.replog.ReplicatedLogReplica;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HighWaterMarkTest {

    @Test
    void hwm_advances_only_when_no_gaps() {
        ReplicatedLogReplica r = new ReplicatedLogReplica();
        r.setCommitted(1, "V1");
        r.setCommitted(3, "V3");
        assertEquals(1, r.getHighWaterMark());
        r.setCommitted(2, "V2");
        assertEquals(3, r.getHighWaterMark());
    }
}



