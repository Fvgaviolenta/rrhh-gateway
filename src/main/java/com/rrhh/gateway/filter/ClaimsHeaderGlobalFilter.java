package com.rrhh.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Propaga claims del JWT como headers hacia los microservicios.
 * Usa {@link ServerHttpRequestDecorator} porque {@code request.mutate().header(...)}
 * falla con {@link UnsupportedOperationException} sobre {@code ReadOnlyHttpHeaders}
 * (Spring Web 6.1 + Security resource server).
 */
@Component
public class ClaimsHeaderGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    if (ctx.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                        var jwt = jwtAuth.getToken();
                        String tenantId = firstClaim(jwt.getClaimAsString("custom:tenant_id"), jwt.getClaimAsString("tenant_id"));
                        String userId = firstClaim(jwt.getClaimAsString("custom:user_id"), jwt.getClaimAsString("user_id"));
                        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
                        if (requestId == null || requestId.isBlank()) {
                            requestId = UUID.randomUUID().toString();
                        }
                        ServerHttpRequest decorated = withClaimHeaders(
                                exchange.getRequest(),
                                tenantId != null ? tenantId : "",
                                userId != null ? userId : "",
                                requestId
                        );
                        return chain.filter(exchange.mutate().request(decorated).build());
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    private static ServerHttpRequest withClaimHeaders(
            ServerHttpRequest original,
            String tenantId,
            String userId,
            String requestId
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(original.getHeaders());
        headers.set("X-Tenant-Id", tenantId);
        headers.set("X-User-Id", userId);
        headers.set("X-Request-Id", requestId);
        HttpHeaders readOnly = HttpHeaders.readOnlyHttpHeaders(headers);
        return new ServerHttpRequestDecorator(original) {
            @Override
            public HttpHeaders getHeaders() {
                return readOnly;
            }
        };
    }

    private static String firstClaim(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
