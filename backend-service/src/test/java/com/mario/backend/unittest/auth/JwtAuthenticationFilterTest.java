package com.mario.backend.unittest.auth;

import com.mario.backend.auth.security.AuthenticatedUser;
import com.mario.backend.auth.security.JwtAuthenticationFilter;
import com.mario.backend.auth.security.JwtTokenProvider;
import com.mario.backend.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static com.mario.backend.testutil.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validAccessToken_setsAuthentication() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(token)).thenReturn("access");
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(USER_ID);
        when(jwtTokenProvider.getEmailFromToken(token)).thenReturn(USER_EMAIL);
        when(jwtTokenProvider.getRoleFromToken(token)).thenReturn(ROLE_BASIC_USER);
        when(jwtTokenProvider.getPermissionsFromToken(token)).thenReturn(BASIC_USER_PERMISSIONS);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(USER_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void noAuthorizationHeader_doesNotSetAuthentication() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void blacklistedToken_doesNotSetAuthentication() throws ServletException, IOException {
        String token = "blacklisted-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_doesNotSetAuthentication() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}