package com.shop.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.api.ApiResponse;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.CommonConstants;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class BaseClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private TestClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        client = new TestClient("http://localhost:" + port, "internal-secret", objectMapper);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        server.stop(0);
    }

    @Test
    void injectsInternalTokenAndRequestIdHeaders() {
        AtomicReference<String> receivedToken = new AtomicReference<>();
        AtomicReference<String> receivedReqId = new AtomicReference<>();

        server.createContext("/ok", exchange -> {
            receivedToken.set(exchange.getRequestHeaders().getFirst(CommonConstants.HEADER_INTERNAL_TOKEN));
            receivedReqId.set(exchange.getRequestHeaders().getFirst(CommonConstants.HEADER_REQUEST_ID));
            byte[] body = "{\"success\":true,\"data\":\"pong\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        MDC.put(CommonConstants.MDC_REQUEST_ID, "req-123");
        String result = client.ping();

        assertThat(result).contains("pong");
        assertThat(receivedToken.get()).isEqualTo("internal-secret");
        assertThat(receivedReqId.get()).isEqualTo("req-123");
    }

    @Test
    void mapsUpstreamApiErrorToBusinessException() {
        server.createContext("/fail", exchange -> {
            byte[] body = objectMapper.writeValueAsBytes(
                ApiResponse.error(ErrorCode.STOCK_INSUFFICIENT.name(), "Out of stock")
            );
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(409, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        assertThatThrownBy(() -> client.fail())
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getErrorCode()).isEqualTo(ErrorCode.STOCK_INSUFFICIENT);
                assertThat(be.getMessage()).isEqualTo("Out of stock");
            });
    }

    static class TestClient extends BaseClient {
        TestClient(String baseUrl, String internalToken, ObjectMapper objectMapper) {
            super(baseUrl, internalToken, objectMapper);
        }

        String ping() {
            return restClient.get().uri("/ok").retrieve().body(String.class);
        }

        String fail() {
            return restClient.get().uri("/fail").retrieve().body(String.class);
        }
    }
}
