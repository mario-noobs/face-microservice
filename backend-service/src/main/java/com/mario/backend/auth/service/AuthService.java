package com.mario.backend.auth.service;

import com.mario.backend.auth.dto.*;
import com.mario.backend.auth.entity.Auth;
import com.mario.backend.auth.repository.AuthRepository;
import com.mario.backend.auth.security.JwtTokenProvider;
import com.mario.backend.common.exception.ApiException;
import com.mario.backend.users.entity.User;
import com.mario.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .status(User.UserStatus.activated)
                .role(User.UserRole.user)
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

        return generateTokenResponse(user.getId(), request.getEmail());
    }

    public TokenResponse login(LoginRequest request) {
        Auth auth = authRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));

        if (!BCrypt.checkpw(request.getPassword(), auth.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }

        return generateTokenResponse(auth.getUserId(), auth.getEmail());
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired refresh token");
        }

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_BLACKLISTED", "Refresh token has been revoked");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_TYPE", "Expected refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        tokenBlacklistService.blacklistToken(refreshToken, jwtTokenProvider.getExpirationFromToken(refreshToken));

        return generateTokenResponse(userId, email);
    }

    public void logout(LogoutRequest request) {
        String accessToken = request.getAccessToken();

        if (jwtTokenProvider.validateToken(accessToken)) {
            tokenBlacklistService.blacklistToken(accessToken, jwtTokenProvider.getExpirationFromToken(accessToken));
        }
    }

    private TokenResponse generateTokenResponse(Long userId, String email) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

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
