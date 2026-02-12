package com.mario.backend.auth.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class AuthenticatedUser {
    private final Long userId;
    private final String email;
    private final String roleName;
    private final List<String> permissions;

    public AuthenticatedUser(Long userId, String email) {
        this(userId, email, null, Collections.emptyList());
    }

    public boolean hasPermission(String permission) {
        return isSuperAdmin() || permissions.contains(permission);
    }

    public boolean isSuperAdmin() {
        return "SUPERADMIN".equals(roleName);
    }
}
