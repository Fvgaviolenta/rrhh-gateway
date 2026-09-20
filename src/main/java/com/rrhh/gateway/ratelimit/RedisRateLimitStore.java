package com.rrhh.gateway.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Ventana fija de 1 s en Redis. Si Redis falla, delega al almacén en memoria.
 */
public class RedisRateLimitStore implements RateLimitStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitStore.class);

    private final ReactiveStringRedisTemplate redis;
    private final RateLimitStore fallback;

    public RedisRateLimitStore(ReactiveStringRedisTemplate redis, RateLimitStore fallback) {
        this.redis = redis;
        this.fallback = fallback;
    }

    @Override
    public Mono<Boolean> tryConsume(String clientKey, int replenishPerSecond, int burst) {
        int cap = Math.max(burst, replenishPerSecond);
        String key = "rrhh:rl:" + clientKey + ":" + (System.currentTimeMillis() / 1000);
        return redis.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> expire = count == 1
                            ? redis.expire(key, Duration.ofSeconds(2)).thenReturn(true)
                            : Mono.just(true);
                    return expire.thenReturn(count <= cap);
                })
                .onErrorResume(ex -> {
                    log.warn("Redis rate-limit no disponible, se usa memoria: {}", ex.getMessage());
                    return fallback.tryConsume(clientKey, replenishPerSecond, burst);
                });
    }
}
