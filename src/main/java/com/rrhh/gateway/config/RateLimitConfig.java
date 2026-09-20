package com.rrhh.gateway.config;

import com.rrhh.gateway.ratelimit.InMemoryRateLimitStore;
import com.rrhh.gateway.ratelimit.RateLimitStore;
import com.rrhh.gateway.ratelimit.RedisRateLimitStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public InMemoryRateLimitStore inMemoryRateLimitStore() {
        return new InMemoryRateLimitStore();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "rrhh.rate-limit.backend", havingValue = "memory")
    public RateLimitStore memoryRateLimitStore(InMemoryRateLimitStore inMemory) {
        return inMemory;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(ReactiveStringRedisTemplate.class)
    @ConditionalOnProperty(name = "rrhh.rate-limit.backend", havingValue = "auto", matchIfMissing = true)
    public RateLimitStore memoryWhenNoRedis(InMemoryRateLimitStore inMemory) {
        return inMemory;
    }

    @Bean
    @Primary
    @ConditionalOnBean(ReactiveStringRedisTemplate.class)
    @ConditionalOnProperty(name = "rrhh.rate-limit.backend", havingValue = "auto", matchIfMissing = true)
    public RateLimitStore redisRateLimitStore(
            ReactiveStringRedisTemplate redis,
            InMemoryRateLimitStore inMemory
    ) {
        return new RedisRateLimitStore(redis, inMemory);
    }
}
