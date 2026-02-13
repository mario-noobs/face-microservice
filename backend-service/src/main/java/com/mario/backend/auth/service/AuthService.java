package com.mario.backend.auth.service;

import com.mario.backend.auth.dto.*;
import com.mario.backend.auth.entity.Auth;
import com.mario.backend.logging.annotation.Traceable;
import com.mario.backend.auth.repository.AuthRepository;
import com.mario.backend.auth.security.JwtTokenProvider;
import com.mario.backend.common.exception.ApiException;
import com.mario.backend.common.exception.ErrorCode;
import com.mario.backend.rbac.entity.Permission;
import com.mario.backend.rbac.entity.Role;
import com.mario.backend.rbac.repository.RoleRepository;
import com.mario.backend.users.entity.User;
import com.mario.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Traceable("auth.register")
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS);
        }

        Role defaultRole = roleRepository.findByIsDefaultTrue()
                .orElseGet(() -> roleRepository.findByName("BASIC_USER").orElse(null));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .status(User.UserStatus.activated)
                .role(defaultRole)
                .build();
        user = userRepository.save(user);

        String salt = BCrypt.gensalt();
        String hashedPassword = BCrypt.hashpw(request.getPassword(), salt);

        Auth auth = Auth.builder()
                .userId(user.getId())
                .email(request.getEmail())
                .salt(salt)
                .password(hashedPassword)
                .authType(Auth.AuthType.email_password)
                .build();
        authRepository.save(auth);

        return generateTokenResponse(user);
    }

    @Traceable("auth.login")
    public TokenResponse login(LoginRequest request) {
        Auth auth = authRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!BCrypt.checkpw(request.getPassword(), auth.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userRepository.findById(auth.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS, "User not found"));

        return generateTokenResponse(user);
    }

    @Traceable("auth.refreshToken")
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN, "Invalid or expired refresh token");
        }

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new ApiException(ErrorCode.TOKEN_BLACKLISTED, "Refresh token has been revoked");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN_TYPE, "Expected refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        tokenBlacklistService.blacklistToken(refreshToken, jwtTokenProvider.getExpirationFromToken(refreshToken));

        // Load user from DB to get current role (picks up role changes)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN, "User not found"));

        return generateTokenResponse(user);
    }

    @Traceable("auth.logout")
    public void logout(LogoutRequest request) {
        String accessToken = request.getAccessToken();

        if (jwtTokenProvider.validateToken(accessToken)) {
            tokenBlacklistService.blacklistToken(accessToken, jwtTokenProvider.getExpirationFromToken(accessToken));
        }
    }

    private TokenResponse generateTokenResponse(User user) {
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        List<String> permissions = user.getRole() != null
                ? user.getRole().getPermissions().stream().map(Permission::getName).toList()
                : Collections.emptyList();

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), roleName, permissions);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        return TokenResponse.builder()
                .accessToken(TokenResponse.TokenInfo.builder()
                        .token(accessToken)
                        .expiredIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                        .build())
                .refreshToken(TokenResponse.TokenInfo.builder()
                        .token(refreshToken)
                        .expiredIn(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
                        .build())
                .build();
    }
}
