package com.shop.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.api.ApiResponse;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.CommonConstants;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
public abstract class BaseClient {

    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 5000;

    protected final RestClient restClient;
    private final ObjectMapper objectMapper;

    protected BaseClient(String baseUrl, String internalToken, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS));
        requestFactory.setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MS));

        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .requestInterceptor((request, body, execution) -> {
                if (StringUtils.hasText(internalToken)) {
                    request.getHeaders().set(CommonConstants.HEADER_INTERNAL_TOKEN, internalToken);
                }
                String requestId = MDC.get(CommonConstants.MDC_REQUEST_ID);
                if (StringUtils.hasText(requestId)) {
                    request.getHeaders().set(CommonConstants.HEADER_REQUEST_ID, requestId);
                }
                return execution.execute(request, body);
            })
            .defaultStatusHandler(HttpStatusCode::isError, this::handleErrorResponse)
            .build();
    }

    private void handleErrorResponse(HttpRequest request, ClientHttpResponse response) throws IOException {
        byte[] body = response.getBody().readAllBytes();
        if (body.length > 0) {
            try {
                ApiResponse<?> apiError = objectMapper.readValue(body, ApiResponse.class);
                if (apiError.error() != null && StringUtils.hasText(apiError.error().code())) {
                    ErrorCode code = parseErrorCode(apiError.error().code());
                    List<String> details = apiError.error().details();
                    throw new BusinessException(code, apiError.error().message(), details);
                }
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception parseEx) {
                log.warn("Failed to parse upstream error body from {}", request.getURI(), parseEx);
            }
        }
        throw new BusinessException(ErrorCode.UPSTREAM_ERROR, "Upstream call failed with status " + response.getStatusCode());
    }

    private ErrorCode parseErrorCode(String code) {
        try {
            return ErrorCode.valueOf(code);
        } catch (IllegalArgumentException ex) {
            return ErrorCode.UPSTREAM_ERROR;
        }
    }
}
