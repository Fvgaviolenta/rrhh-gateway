package com.rrhh.gateway.ratelimit;

import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Cubo de tokens en memoria (tests y fallback si Redis no está).
 */
public class InMemoryRateLimitStore implements RateLimitStore {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> tryConsume(String clientKey, int replenishPerSecond, int burst) {
        return Mono.fromSupplier(() -> {
            Bucket bucket = buckets.computeIfAbsent(clientKey, k -> new Bucket(burst));
            return bucket.tryConsume(replenishPerSecond, burst);
        });
    }

    private static final class Bucket {
        private double tokens;
        private long lastNanos;

        private Bucket(int burst) {
            this.tokens = burst;
            this.lastNanos = System.nanoTime();
        }

        private synchronized boolean tryConsume(int replenishPerSecond, int burst) {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastNanos) / 1_000_000_000.0;
            tokens = Math.min(burst, tokens + elapsedSeconds * replenishPerSecond);
            lastNanos = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
