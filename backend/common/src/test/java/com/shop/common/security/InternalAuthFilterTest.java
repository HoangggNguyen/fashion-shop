package com.shop.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalAuthFilterTest {

    private final InternalAuthFilter filter = new InternalAuthFilter("secret-token", new ObjectMapper());

    @Test
    void internalEndpointRejectsMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new AssertionError("Filter chain should not be called");
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }

    @Test
    void validInternalTokenPopulatesAndClearsUserContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.addHeader(CommonConstants.HEADER_INTERNAL_TOKEN, "secret-token");
        request.addHeader(CommonConstants.HEADER_USER_ID, "42");
        request.addHeader(CommonConstants.HEADER_USER_ROLES, "CUSTOMER,ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<UserPrincipal> captured = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> captured.set(UserContext.get().orElse(null)));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().userId()).isEqualTo(42L);
        assertThat(captured.get().roles()).containsExactlyInAnyOrder("CUSTOMER", "ADMIN");
        assertThat(UserContext.get()).isEmpty();
    }

    @Test
    void spoofedUserHeaderWithoutInternalTokenIsIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader(CommonConstants.HEADER_USER_ID, "999");
        request.addHeader(CommonConstants.HEADER_USER_ROLES, "ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<UserPrincipal> captured = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> captured.set(UserContext.get().orElse(null)));

        assertThat(captured.get()).isNull();
    }
}
