package com.rrhh.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Configuration
public class GatewayErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorConfig.class);

    @Bean
    @Order(-2)
    public ErrorWebExceptionHandler gatewayErrorHandler() {
        return (ServerWebExchange exchange, Throwable ex) -> {
            HttpStatus status = resolveStatus(ex);
            String path = exchange.getRequest().getPath().value();
            if (status.is5xxServerError()) {
                log.error("Gateway 5xx en {} → {}: {}", path, ex.getClass().getName(), ex.getMessage(), ex);
            } else {
                log.warn("Gateway {} en {} → {}: {}", status.value(), path, ex.getClass().getName(), ex.getMessage());
            }
            exchange.getResponse().setStatusCode(status);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String mensaje = status.is5xxServerError() && ex.getMessage() != null
                    ? escape(ex.getMessage())
                    : escape(status.getReasonPhrase());
            String body = String.format(
                    "{\"codigo\":%d,\"mensaje\":\"%s\",\"datos\":null,\"errores\":[]}",
                    status.value(),
                    mensaje
            );
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        };
    }

    private static HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof InvalidBearerTokenException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ex instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        }
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String escape(String value) {
        return value == null ? "Error" : value.replace("\\", "\\\\").replace("\"", "'").replace("\n", " ").replace("\r", " ");
    }
}
