package com.shop.gateway.security;

import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class PublicRouteMatcher {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final List<String> PUBLIC_AUTH_POST = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password"
    );

    private static final List<String> PUBLIC_GET_PATTERNS = List.of(
        "/actuator/health",
        "/actuator/info",
        "/api/v1/products/**",
        "/api/v1/categories/**",
        "/api/v1/brands/**",
        "/api/v1/reviews/**",
        "/api/v1/coupons/public",
        "/api/v1/payments/vnpay/return",
        "/api/v1/payments/vnpay/ipn",
        "/uploads/**"
    );

    public boolean isPublic(HttpMethod method, String path) {
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }
        if (HttpMethod.POST.equals(method) && PUBLIC_AUTH_POST.stream().anyMatch(p -> MATCHER.match(p, path))) {
            return true;
        }
        if (HttpMethod.GET.equals(method) && PUBLIC_GET_PATTERNS.stream().anyMatch(p -> MATCHER.match(p, path))) {
            return true;
        }
        return false;
    }
}
