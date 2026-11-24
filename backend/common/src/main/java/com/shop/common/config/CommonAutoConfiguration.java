package com.shop.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.GlobalExceptionHandler;
import com.shop.common.security.CommonWebConfig;
import com.shop.common.security.InternalAuthFilter;
import com.shop.common.security.RequireRoleInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequireRoleInterceptor requireRoleInterceptor() {
        return new RequireRoleInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public CommonWebConfig commonWebConfig(RequireRoleInterceptor requireRoleInterceptor) {
        return new CommonWebConfig(requireRoleInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean(InternalAuthFilter.class)
    public FilterRegistrationBean<InternalAuthFilter> internalAuthFilterRegistration(
        @Value("${app.internal-token:dev-internal-token}") String internalToken,
        ObjectMapper objectMapper
    ) {
        FilterRegistrationBean<InternalAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new InternalAuthFilter(internalToken, objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
