package com.mario.backend.gateway.filter;

import com.mario.backend.audit.entity.AuditLog;
import com.mario.backend.audit.service.AuditService;
import com.mario.backend.auth.security.AuthenticatedUser;
import com.mario.backend.logging.context.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditService auditService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        TraceContext.setTraceId(requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            Long userId = extractUserId();
            if (userId != null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String email = (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u)
                        ? u.getEmail() : null;
                TraceContext.setUser(userId, email);
            }

            AuditLog auditLog = AuditLog.builder()
                    .requestId(requestId)
                    .userId(userId)
                    .method(request.getMethod())
                    .path(request.getRequestURI())
                    .statusCode(response.getStatus())
                    .clientIp(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .durationMs(duration)
                    .build();

            auditService.saveAuditLog(auditLog);

            log.info("Request completed: {} {} {} {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);

            TraceContext.clear();
        }
    }

    private Long extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.getUserId();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.equals("/ping");
    }
}
