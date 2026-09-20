package com.rrhh.gateway.filter;

import com.rrhh.gateway.config.RateLimitProperties;
import com.rrhh.gateway.ratelimit.RateLimitStore;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@Component
public class RateLimitWebFilter implements WebFilter, Ordered {

    private final RateLimitStore store;
    private final RateLimitProperties properties;

    public RateLimitWebFilter(RateLimitStore store, RateLimitProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() == HttpMethod.OPTIONS || isExempt(request.getPath().value())) {
            return chain.filter(exchange);
        }
        return resolveClientKey(exchange)
                .flatMap(key -> store.tryConsume(key, properties.getReplenish(), properties.getBurst()))
                .flatMap(allowed -> allowed ? chain.filter(exchange) : tooMany(exchange));
    }

    private static boolean isExempt(String path) {
        return path.startsWith("/actuator")
                || path.equals("/api/v1/tenants/resolver")
                || path.equals("/api/v1/catalogo");
    }

    private static Mono<String> resolveClientKey(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> {
                    if (ctx.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                        String sub = jwtAuth.getToken().getSubject();
                        if (sub != null && !sub.isBlank()) {
                            return "sub:" + sub;
                        }
                    }
                    return ipKey(exchange);
                })
                .defaultIfEmpty(ipKey(exchange));
    }

    private static String ipKey(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        String ip = remote != null && remote.getAddress() != null
                ? remote.getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ip;
    }

    private static Mono<Void> tooMany(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = """
                {"codigo":429,"mensaje":"Demasiadas solicitudes. Reintenta en un momento.","datos":null,"errores":[]}
                """.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
