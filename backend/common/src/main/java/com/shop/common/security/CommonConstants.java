package com.shop.common.security;

public final class CommonConstants {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final String MDC_REQUEST_ID = "requestId";

    private CommonConstants() {
    }
}
