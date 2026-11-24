package com.shop.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.api.ApiResponse;
import com.shop.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalAuthFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PREFIX = "/internal/";

    private final String internalToken;
    private final ObjectMapper objectMapper;

    public InternalAuthFilter(String internalToken, ObjectMapper objectMapper) {
        this.internalToken = internalToken;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(CommonConstants.MDC_REQUEST_ID, requestId);
        response.setHeader(CommonConstants.HEADER_REQUEST_ID, requestId);

        try {
            String suppliedToken = request.getHeader(CommonConstants.HEADER_INTERNAL_TOKEN);
            boolean tokenValid = StringUtils.hasText(internalToken) && internalToken.equals(suppliedToken);

            if (request.getRequestURI().startsWith(INTERNAL_PREFIX) && !tokenValid) {
                writeError(response, ErrorCode.UNAUTHORIZED, "Invalid internal token");
                return;
            }

            String userIdHeader = request.getHeader(CommonConstants.HEADER_USER_ID);
            if (tokenValid && StringUtils.hasText(userIdHeader)) {
                Long userId = parseUserId(userIdHeader);
                if (userId == null) {
                    writeError(response, ErrorCode.UNAUTHORIZED, "Invalid user header");
                    return;
                }
                Set<String> roles = parseRoles(request.getHeader(CommonConstants.HEADER_USER_ROLES));
                UserContext.set(new UserPrincipal(userId, roles));
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            MDC.remove(CommonConstants.MDC_REQUEST_ID);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String header = request.getHeader(CommonConstants.HEADER_REQUEST_ID);
        return StringUtils.hasText(header) ? header : UUID.randomUUID().toString();
    }

    private Long parseUserId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) {
            return Set.of();
        }
        return Arrays.stream(rolesHeader.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .collect(Collectors.toUnmodifiableSet());
    }

    private void writeError(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code.name(), message));
    }
}
