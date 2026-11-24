package com.shop.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shop.common.exception.ForbiddenException;
import com.shop.common.exception.UnauthorizedException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class RequireRoleInterceptorTest {

    private final RequireRoleInterceptor interceptor = new RequireRoleInterceptor();
    private final DummyController controller = new DummyController();

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    @Test
    void throwsUnauthorizedWhenNoUserInContext() throws Exception {
        HandlerMethod handler = new HandlerMethod(controller, DummyController.class.getMethod("adminOnly"));
        assertThatThrownBy(() -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), handler))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void throwsForbiddenWhenRoleDoesNotMatch() throws Exception {
        UserContext.set(new UserPrincipal(1L, Set.of(CommonConstants.ROLE_CUSTOMER)));
        HandlerMethod handler = new HandlerMethod(controller, DummyController.class.getMethod("adminOnly"));
        assertThatThrownBy(() -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), handler))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void allowsRequestWhenRoleMatches() throws Exception {
        UserContext.set(new UserPrincipal(1L, Set.of(CommonConstants.ROLE_ADMIN)));
        HandlerMethod handler = new HandlerMethod(controller, DummyController.class.getMethod("adminOnly"));
        boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), handler);
        assertThat(allowed).isTrue();
    }

    static class DummyController {
        @RequireRole(CommonConstants.ROLE_ADMIN)
        public void adminOnly() {
        }
    }
}
