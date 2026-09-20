package com.rrhh.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class AccessLogWebFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AccessLogWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long start = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String headerRequestId = request.getHeaders().getFirst("X-Request-Id");
        final String requestId = (headerRequestId == null || headerRequestId.isBlank())
                ? UUID.randomUUID().toString()
                : headerRequestId;
        final ServerWebExchange next;
        if (headerRequestId == null || headerRequestId.isBlank()) {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(request.getHeaders());
            headers.set("X-Request-Id", requestId);
            HttpHeaders readOnly = HttpHeaders.readOnlyHttpHeaders(headers);
            ServerHttpRequest decorated = new ServerHttpRequestDecorator(request) {
                @Override
                public HttpHeaders getHeaders() {
                    return readOnly;
                }
            };
            next = exchange.mutate().request(decorated).build();
        } else {
            next = exchange;
        }
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "?";
        return chain.filter(next)
                .doFinally(signal -> {
                    int status = next.getResponse().getStatusCode() != null
                            ? next.getResponse().getStatusCode().value()
                            : 200;
                    String tenant = next.getRequest().getHeaders().getFirst("X-Tenant-Id");
                    log.info(
                            "api_manager method={} path={} status={} duration_ms={} request_id={} tenant_id={}",
                            method,
                            path,
                            status,
                            System.currentTimeMillis() - start,
                            requestId,
                            tenant != null && !tenant.isBlank() ? tenant : "-"
                    );
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
