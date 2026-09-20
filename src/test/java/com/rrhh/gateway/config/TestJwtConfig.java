package com.rrhh.gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.time.Instant;

@TestConfiguration
public class TestJwtConfig {

    @Bean
    @Primary
    ReactiveJwtDecoder reactiveJwtDecoder() {
        return token -> {
            Instant now = Instant.now();
            Jwt.Builder builder = Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600));
            switch (token) {
                case "operador" -> builder.subject("operador")
                        .claim("email", "operador@rrhh.local")
                        .claim("custom:role", "OperadorSaaS")
                        .claim("custom:user_id", "dddddddd-dddd-dddd-dddd-dddddddddddd");
                case "trabajador" -> builder.subject("trabajador")
                        .claim("email", "trab@rrhh.local")
                        .claim("custom:tenant_id", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                        .claim("custom:role", "Trabajador");
                default -> builder.subject("admin")
                        .claim("email", "admin.demo@rrhh.local")
                        .claim("custom:tenant_id", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                        .claim("custom:role", "Admin de RRHH")
                        .claim("custom:user_id", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
            }
            return Mono.just(builder.build());
        };
    }
}
