package com.example.tickloomworkshop.foundations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LittlesLawTest {

    @Test
    void concurrent_users_from_littles_law() {
        double lambda = 5000.0;   // req/s
        double T = 0.2;           // seconds
        double L = lambda * T;
        assertEquals(1000.0, L, 1e-9);
    }

    @Test
    void decompose_total_time_into_wait_and_service() {
        double S = 0.050; // 50 ms service time
        double T = 0.180; // 180 ms total time in system
        double Wq = T - S;
        assertEquals(0.130, Wq, 1e-9);
    }
}




