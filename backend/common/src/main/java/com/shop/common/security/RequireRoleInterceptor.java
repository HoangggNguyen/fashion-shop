package com.shop.common.security;

import com.shop.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RequireRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequireRole annotation = method.getMethodAnnotation(RequireRole.class);
        if (annotation == null) {
            annotation = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (annotation == null) {
            return true;
        }

        UserPrincipal principal = UserContext.require();
        boolean allowed = Arrays.stream(annotation.value()).anyMatch(principal::hasRole);
        if (!allowed) {
            throw new ForbiddenException("Insufficient role permissions");
        }
        return true;
    }
}
