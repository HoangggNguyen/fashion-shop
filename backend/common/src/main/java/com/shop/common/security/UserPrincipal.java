package com.shop.common.security;

import java.util.Set;

public record UserPrincipal(
    Long userId,
    Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(CommonConstants.ROLE_ADMIN);
    }
}
