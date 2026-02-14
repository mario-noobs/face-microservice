package com.mario.backend.unittest.auth;

import com.mario.backend.auth.security.JwtTokenProvider;
import com.mario.backend.testutil.JwtTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.mario.backend.testutil.TestConstants.*;
import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private JwtTestHelper helper;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(JWT_SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
        helper = new JwtTestHelper();
    }

    @Test
    void generateAccessToken_containsCorrectClaims() {
        String token = provider.generateAccessToken(USER_ID, USER_EMAIL, ROLE_BASIC_USER, BASIC_USER_PERMISSIONS);

        assertThat(provider.getUserIdFromToken(token)).isEqualTo(USER_ID);
        assertThat(provider.getEmailFromToken(token)).isEqualTo(USER_EMAIL);
        assertThat(provider.getTokenType(token)).isEqualTo("access");
        assertThat(provider.getRoleFromToken(token)).isEqualTo(ROLE_BASIC_USER);
        assertThat(provider.getPermissionsFromToken(token)).containsExactlyInAnyOrderElementsOf(BASIC_USER_PERMISSIONS);
    }

    @Test
    void generateRefreshToken_containsCorrectClaims() {
        String token = provider.generateRefreshToken(USER_ID, USER_EMAIL);

        assertThat(provider.getUserIdFromToken(token)).isEqualTo(USER_ID);
        assertThat(provider.getTokenType(token)).isEqualTo("refresh");
        assertThat(provider.getRoleFromToken(token)).isNull();
        assertThat(provider.getPermissionsFromToken(token)).isEmpty();
    }

    @Test
    void validateToken_validAccessToken_returnsTrue() {
        String token = provider.generateAccessToken(USER_ID, USER_EMAIL, ROLE_BASIC_USER, BASIC_USER_PERMISSIONS);
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        String token = helper.generateExpiredAccessToken();
        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_wrongSignature_returnsFalse() {
        String token = helper.generateTokenWithWrongSignature();
        assertThat(provider.validateToken(token)).isFalse();
    }
}
