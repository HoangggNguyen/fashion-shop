package com.shop.common.security;

import com.shop.common.exception.UnauthorizedException;
import java.util.Optional;

public final class UserContext {

    private static final ThreadLocal<UserPrincipal> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserPrincipal principal) {
        CURRENT.set(principal);
    }

    public static Optional<UserPrincipal> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UserPrincipal require() {
        return get().orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    public static Long requireUserId() {
        return require().userId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
