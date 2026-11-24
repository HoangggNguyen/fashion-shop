package com.shop.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class InternalBlockWebFilter implements WebFilter, Ordered {

    private final ObjectMapper objectMapper;

    public InternalBlockWebFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/internal/")) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = Map.of(
                "success", false,
                "error", Map.of("code", "FORBIDDEN", "message", "Internal endpoints are not exposed", "details", List.of())
            );
            byte[] bytes;
            try {
                bytes = objectMapper.writeValueAsBytes(payload);
            } catch (JsonProcessingException ex) {
                bytes = "{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Internal endpoints are not exposed\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            }
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange);
    }
}
