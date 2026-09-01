package com.rrhh.gateway.filter;

import com.rrhh.gateway.security.Roles;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@Order(-1)
public class RoleAuthorizationWebFilter implements WebFilter {

    private static final Set<String> ADMIN_ROLES = Set.of(Roles.SUPERADMIN, Roles.ADMIN_RRHH);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();

        if (!path.startsWith("/api/v1/") || method == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        if (!requiresAdmin(path, method)) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    if (!(ctx.getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
                        return forbidden(exchange);
                    }
                    Jwt jwt = jwtAuth.getToken();
                    String role = firstClaim(jwt, "custom:role", "role");
                    String authority = Roles.authorityFromClaim(role);
                    if (ADMIN_ROLES.contains(authority)) {
                        return chain.filter(exchange);
                    }
                    return forbidden(exchange);
                })
                .switchIfEmpty(forbidden(exchange));
    }

    private static boolean requiresAdmin(String path, HttpMethod method) {
        if (path.startsWith("/api/v1/usuarios")) {
            return true;
        }
        if (path.startsWith("/api/v1/trabajadores") && method != HttpMethod.GET) {
            return true;
        }
        if (path.startsWith("/api/v1/contratos") && (method == HttpMethod.POST || method == HttpMethod.PATCH)) {
            return true;
        }
        if (path.startsWith("/api/v1/liquidaciones") && method == HttpMethod.POST) {
            return true;
        }
        return false;
    }

    private static Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = """
                {"codigo":403,"mensaje":"Acceso denegado por rol","datos":null,"errores":[]}
                """.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private static String firstClaim(Jwt jwt, String... names) {
        for (String name : names) {
            String value = jwt.getClaimAsString(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
