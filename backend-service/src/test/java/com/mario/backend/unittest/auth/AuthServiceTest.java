package com.mario.backend.unittest.auth;

import com.mario.backend.auth.dto.*;
import com.mario.backend.auth.entity.Auth;
import com.mario.backend.auth.repository.AuthRepository;
import com.mario.backend.auth.security.JwtTokenProvider;
import com.mario.backend.auth.service.AuthService;
import com.mario.backend.auth.service.TokenBlacklistService;
import com.mario.backend.common.exception.ApiException;
import com.mario.backend.rbac.entity.Role;
import com.mario.backend.rbac.repository.RoleRepository;
import com.mario.backend.testutil.TestDataFactory;
import com.mario.backend.users.entity.User;
import com.mario.backend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Date;
import java.util.Optional;

import static com.mario.backend.testutil.TestConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthRepository authRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Role defaultRole;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .firstName(USER_FIRST_NAME).lastName(USER_LAST_NAME)
                .email(USER_EMAIL).password(USER_PASSWORD).build();
        loginRequest = LoginRequest.builder()
                .email(USER_EMAIL).password(USER_PASSWORD).build();
        defaultRole = TestDataFactory.createBasicUserRole();
    }

    @Test
    void register_success() {
        when(authRepository.existsByEmail(USER_EMAIL)).thenReturn(false);
        when(roleRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0); u.setId(USER_ID); return u;
        });
        when(authRepository.save(any(Auth.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(eq(USER_ID), eq(USER_EMAIL), anyString(), anyList()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(USER_ID, USER_EMAIL)).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        TokenResponse response = authService.register(registerRequest);

        assertThat(response.getAccessToken().getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken().getToken()).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
        verify(authRepository).save(any(Auth.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(authRepository.existsByEmail(USER_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("EMAIL_EXISTS");
    }

    @Test
    void login_success() {
        String salt = BCrypt.gensalt();
        String hashed = BCrypt.hashpw(USER_PASSWORD, salt);
        Auth auth = Auth.builder().id(1L).userId(USER_ID).email(USER_EMAIL).salt(salt).password(hashed).build();
        User user = TestDataFactory.createUser();

        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(auth));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString(), anyList()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        TokenResponse response = authService.login(loginRequest);
        assertThat(response.getAccessToken().getToken()).isEqualTo("access-token");
    }

    @Test
    void login_wrongEmail_throwsUnauthorized() {
        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        String salt = BCrypt.gensalt();
        String hashed = BCrypt.hashpw("DifferentPassword", salt);
        Auth auth = Auth.builder().id(1L).userId(USER_ID).email(USER_EMAIL).salt(salt).password(hashed).build();
        when(authRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(auth));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void refreshToken_success() {
        String token = "valid-refresh-token";
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(token).build();
        User user = TestDataFactory.createUser();

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(token)).thenReturn("refresh");
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(USER_ID);
        when(jwtTokenProvider.getExpirationFromToken(token)).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString(), anyList()))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        TokenResponse response = authService.refreshToken(request);
        assertThat(response.getAccessToken().getToken()).isEqualTo("new-access-token");
        verify(tokenBlacklistService).blacklistToken(eq(token), any(Date.class));
    }

    @Test
    void refreshToken_invalidToken_throwsUnauthorized() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("invalid").build();
        when(jwtTokenProvider.validateToken("invalid")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    void logout_validToken_blacklists() {
        String token = "valid-access-token";
        LogoutRequest request = LogoutRequest.builder().accessToken(token).build();
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getExpirationFromToken(token)).thenReturn(new Date(System.currentTimeMillis() + 10000));

        authService.logout(request);
        verify(tokenBlacklistService).blacklistToken(eq(token), any(Date.class));
    }
}
