package com.mario.backend.testutil;

import com.mario.backend.auth.security.JwtTokenProvider;

import java.util.Collections;
import java.util.List;

import static com.mario.backend.testutil.TestConstants.*;

public class JwtTestHelper {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtTestHelper() {
        this.jwtTokenProvider = new JwtTokenProvider(
                JWT_SECRET,
                ACCESS_TOKEN_EXPIRATION,
                REFRESH_TOKEN_EXPIRATION
        );
    }

    public JwtTokenProvider getProvider() {
        return jwtTokenProvider;
    }

    public String generateValidAccessToken() {
        return generateValidAccessToken(USER_ID, USER_EMAIL, ROLE_BASIC_USER, BASIC_USER_PERMISSIONS);
    }

    public String generateValidAccessToken(Long userId, String email, String role, List<String> permissions) {
        return jwtTokenProvider.generateAccessToken(userId, email, role, permissions);
    }

    public String generateSuperAdminAccessToken() {
        return jwtTokenProvider.generateAccessToken(ADMIN_USER_ID, ADMIN_EMAIL, ROLE_SUPERADMIN, SUPERADMIN_PERMISSIONS);
    }

    public String generatePremiumUserAccessToken() {
        return jwtTokenProvider.generateAccessToken(USER_ID, USER_EMAIL, ROLE_PREMIUM_USER, PREMIUM_USER_PERMISSIONS);
    }

    public String generateValidRefreshToken() {
        return jwtTokenProvider.generateRefreshToken(USER_ID, USER_EMAIL);
    }

    public String generateExpiredAccessToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(JWT_SECRET, -1000L, -1000L);
        return expiredProvider.generateAccessToken(USER_ID, USER_EMAIL, ROLE_BASIC_USER, BASIC_USER_PERMISSIONS);
    }

    public String generateMalformedToken() {
        return "not.a.valid.jwt.token";
    }

    public String generateTokenWithWrongSignature() {
        String wrongSecret = "d3Jvbmctc2VjcmV0LWtleS1mb3ItdGVzdGluZy13aXRoLWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmctc3RyaW5n";
        JwtTokenProvider wrongProvider = new JwtTokenProvider(wrongSecret, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
        return wrongProvider.generateAccessToken(USER_ID, USER_EMAIL, ROLE_BASIC_USER, BASIC_USER_PERMISSIONS);
    }

    public String generateAccessTokenWithNoPermissions() {
        return jwtTokenProvider.generateAccessToken(USER_ID, USER_EMAIL, ROLE_BASIC_USER, Collections.emptyList());
    }
}
