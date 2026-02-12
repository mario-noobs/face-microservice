package com.mario.backend.auth.security;

import com.mario.backend.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.warn("Attempted use of blacklisted token");
                filterChain.doFilter(request, response);
                return;
            }

            String tokenType = jwtTokenProvider.getTokenType(token);
            if (!"access".equals(tokenType)) {
                log.warn("Non-access token used for authentication");
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            String roleName = jwtTokenProvider.getRoleFromToken(token);
            List<String> permissions = jwtTokenProvider.getPermissionsFromToken(token);

            AuthenticatedUser principal = new AuthenticatedUser(userId, email, roleName, permissions);

            List<GrantedAuthority> authorities = new ArrayList<>();
            if (roleName != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            }
            for (String permission : permissions) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
