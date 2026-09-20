package com.rrhh.gateway.ratelimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimitStoreTest {

    @Test
    void burstSeAgotaYRechaza() {
        InMemoryRateLimitStore store = new InMemoryRateLimitStore();
        for (int i = 0; i < 3; i++) {
            assertTrue(store.tryConsume("cliente", 1, 3).block());
        }
        assertFalse(store.tryConsume("cliente", 1, 3).block());
    }

    @Test
    void clavesDistintasNoCompartenCubo() {
        InMemoryRateLimitStore store = new InMemoryRateLimitStore();
        assertTrue(store.tryConsume("a", 1, 1).block());
        assertTrue(store.tryConsume("b", 1, 1).block());
        assertFalse(store.tryConsume("a", 1, 1).block());
    }
}
