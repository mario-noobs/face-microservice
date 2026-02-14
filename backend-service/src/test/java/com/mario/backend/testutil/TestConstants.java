package com.mario.backend.testutil;

import java.util.List;

public final class TestConstants {

    private TestConstants() {}

    // JWT
    public static final String JWT_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLXRoYXQtaXMtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZw==";
    public static final long ACCESS_TOKEN_EXPIRATION = 3600000L;
    public static final long REFRESH_TOKEN_EXPIRATION = 604800000L;

    // Users
    public static final Long USER_ID = 1L;
    public static final Long ADMIN_USER_ID = 99L;
    public static final String USER_EMAIL = "test@example.com";
    public static final String ADMIN_EMAIL = "admin@example.com";
    public static final String USER_PASSWORD = "Password123";
    public static final String USER_FIRST_NAME = "John";
    public static final String USER_LAST_NAME = "Doe";

    // Roles
    public static final String ROLE_SUPERADMIN = "SUPERADMIN";
    public static final String ROLE_PREMIUM_USER = "PREMIUM_USER";
    public static final String ROLE_BASIC_USER = "BASIC_USER";

    // Permissions
    public static final List<String> SUPERADMIN_PERMISSIONS = List.of(
            "face:register", "face:recognize", "face:delete", "face:check",
            "user:read_self", "user:update_self", "user:read_any", "user:update_any",
            "user:delete_any", "user:list", "audit:read_self", "audit:read_all",
            "rbac:manage_roles", "rbac:assign_roles", "rbac:manage_permissions"
    );

    public static final List<String> PREMIUM_USER_PERMISSIONS = List.of(
            "user:read_self", "user:update_self",
            "face:register", "face:recognize", "face:delete", "face:check",
            "audit:read_self"
    );

    public static final List<String> BASIC_USER_PERMISSIONS = List.of(
            "user:read_self", "user:update_self", "face:check", "audit:read_self"
    );

    // Face
    public static final String SAMPLE_IMAGE_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    public static final String SAMPLE_FACE_ENCODING = "base64encodedfeaturevector";
}
