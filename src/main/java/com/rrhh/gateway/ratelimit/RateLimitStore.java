package com.rrhh.gateway.ratelimit;

import reactor.core.publisher.Mono;

public interface RateLimitStore {

    /**
     * @return true si la petición se admite
     */
    Mono<Boolean> tryConsume(String clientKey, int replenishPerSecond, int burst);
}
