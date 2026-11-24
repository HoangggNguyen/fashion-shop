package com.shop.gateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityTests {

    private static final String JWT_SECRET = "test-jwt-secret-key-at-least-32-bytes-long-12345";
    private static final String INTERNAL_TOKEN = "test-internal-token";

    private static HttpServer downstreamServer;
    private static final AtomicReference<String> lastUserId = new AtomicReference<>();
    private static final AtomicReference<String> lastRoles = new AtomicReference<>();
    private static final AtomicReference<String> lastInternalToken = new AtomicReference<>();
    private static final AtomicReference<String> lastRequestId = new AtomicReference<>();

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startDownstream() throws Exception {
        downstreamServer = HttpServer.create(new InetSocketAddress(0), 0);
        downstreamServer.createContext("/", exchange -> {
            lastUserId.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
            lastRoles.set(exchange.getRequestHeaders().getFirst("X-User-Roles"));
            lastInternalToken.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            lastRequestId.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            byte[] body = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        downstreamServer.start();
    }

    @AfterAll
    static void stopDownstream() {
        if (downstreamServer != null) {
            downstreamServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        String baseUrl = "http://localhost:" + downstreamServer.getAddress().getPort();
        registry.add("CATALOG_URL", () -> baseUrl);
        registry.add("ORDER_URL", () -> baseUrl);
        registry.add("app.jwt.secret", () -> JWT_SECRET);
        registry.add("app.internal-token", () -> INTERNAL_TOKEN);
    }

    @Test
    void blocksInternalEndpoints() {
        webTestClient.get()
            .uri("/internal/v1/users/1")
            .exchange()
            .expectStatus().isForbidden()
            .expectBody()
            .jsonPath("$.error.code").isEqualTo("FORBIDDEN");
    }

    @Test
    void rejectsProtectedRouteWithoutToken() {
        webTestClient.get()
            .uri("/api/v1/orders")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void rejectsInvalidJwtToken() {
        webTestClient.get()
            .uri("/api/v1/orders")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error.code").isEqualTo("TOKEN_INVALID");
    }

    @Test
    void stripsSpoofedHeadersOnPublicRouteAndInjectsInternalToken() {
        lastUserId.set(null);
        lastInternalToken.set(null);
        lastRequestId.set(null);

        webTestClient.get()
            .uri("/api/v1/products")
            .header("X-User-Id", "999")
            .header("X-User-Roles", "ADMIN")
            .header("X-Internal-Token", "fake")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-Request-Id");

        assertThat(lastUserId.get()).isNull();
        assertThat(lastInternalToken.get()).isEqualTo(INTERNAL_TOKEN);
        assertThat(lastRequestId.get()).isNotBlank();
    }

    @Test
    void injectsUserHeadersForValidJwt() {
        String token = Jwts.builder()
            .subject("77")
            .claim("roles", List.of("CUSTOMER"))
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        webTestClient.get()
            .uri("/api/v1/orders")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange()
            .expectStatus().isOk();

        assertThat(lastUserId.get()).isEqualTo("77");
        assertThat(lastRoles.get()).isEqualTo("CUSTOMER");
        assertThat(lastInternalToken.get()).isEqualTo(INTERNAL_TOKEN);
    }
}
