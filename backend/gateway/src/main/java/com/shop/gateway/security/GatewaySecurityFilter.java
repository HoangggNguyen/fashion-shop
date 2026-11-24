package com.shop.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GatewaySecurityFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLES = "X-User-Roles";
    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenVerifier jwtTokenVerifier;
    private final PublicRouteMatcher publicRouteMatcher;
    private final ObjectMapper objectMapper;
    private final String internalToken;

    public GatewaySecurityFilter(
        JwtTokenVerifier jwtTokenVerifier,
        PublicRouteMatcher publicRouteMatcher,
        ObjectMapper objectMapper,
        @Value("${app.internal-token}") String internalToken
    ) {
        this.jwtTokenVerifier = jwtTokenVerifier;
        this.publicRouteMatcher = publicRouteMatcher;
        this.objectMapper = objectMapper;
        this.internalToken = internalToken;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String requestId = resolveRequestId(request);
        exchange.getResponse().getHeaders().set(HEADER_REQUEST_ID, requestId);

        if (path.startsWith("/internal/")) {
            return writeError(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "Internal endpoints are not exposed");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean hasBearer = StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX);
        boolean isPublic = publicRouteMatcher.isPublic(request.getMethod(), path);

        ServerHttpRequest.Builder mutated = request.mutate()
            .headers(headers -> {
                headers.remove(HEADER_USER_ID);
                headers.remove(HEADER_USER_ROLES);
                headers.remove(HEADER_INTERNAL_TOKEN);
                headers.set(HEADER_INTERNAL_TOKEN, internalToken);
                headers.set(HEADER_REQUEST_ID, requestId);
            });

        if (!hasBearer) {
            if (isPublic) {
                return chain.filter(exchange.mutate().request(mutated.build()).build());
            }
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtTokenVerifier.JwtClaims claims = jwtTokenVerifier.verify(token);
            String roles = String.join(",", claims.roles());
            mutated.header(HEADER_USER_ID, claims.userId());
            mutated.header(HEADER_USER_ROLES, roles);
            return chain.filter(exchange.mutate().request(mutated.build()).build());
        } catch (Exception ex) {
            log.debug("JWT verification failed: {}", ex.getMessage());
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Invalid or expired token");
        }
    }

    private String resolveRequestId(ServerHttpRequest request) {
        String existing = request.getHeaders().getFirst(HEADER_REQUEST_ID);
        return StringUtils.hasText(existing) ? existing : UUID.randomUUID().toString();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = Map.of(
            "success", false,
            "error", Map.of("code", code, "message", message, "details", List.of())
        );
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException ex) {
            bytes = ("{\"success\":false,\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}")
                .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
