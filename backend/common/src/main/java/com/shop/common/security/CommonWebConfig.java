package com.shop.common.security;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class CommonWebConfig implements WebMvcConfigurer {

    private final RequireRoleInterceptor requireRoleInterceptor;

    public CommonWebConfig(RequireRoleInterceptor requireRoleInterceptor) {
        this.requireRoleInterceptor = requireRoleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requireRoleInterceptor);
    }
}
