package com.mario.backend.logging.aspect;

import com.mario.backend.auth.security.AuthenticatedUser;
import com.mario.backend.logging.annotation.Traceable;
import com.mario.backend.logging.context.TraceContext;
import com.mario.backend.logging.mask.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Slf4j
@Aspect
@Order(1)
@Component
public class TraceableAspect {

    @Around("@annotation(traceable)")
    public Object trace(ProceedingJoinPoint joinPoint, Traceable traceable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String operation = null;

        // Setup phase - failures should not block business logic
        try {
            operation = traceable.value().isEmpty()
                    ? signature.getDeclaringType().getSimpleName() + "." + signature.getName()
                    : traceable.value();

            enrichMdcFromSecurityContext();
            TraceContext.setOperation(operation);
        } catch (Exception e) {
            log.warn("Failed to setup tracing context: {}", e.getMessage());
            // Continue anyway - don't let logging setup break business logic
        }

        long startNanos = System.nanoTime();

        try {
            // Entry logging - wrapped to prevent blocking business logic
            safeLogEntry(operation, signature, joinPoint.getArgs(), traceable);

            // Execute business logic - this is the only thing that should throw business exceptions
            Object result = joinPoint.proceed();

            // Exit logging - wrapped to prevent successful operations from appearing as failures
            safeLogExit(operation, startNanos, result, traceable);

            return result;

        } catch (Throwable ex) {
            // Error logging - wrapped to prevent suppressing business exceptions
            safeLogError(operation, startNanos, ex);
            throw ex;  // Always re-throw business exceptions
        } finally {
            // Cleanup - wrapped to prevent suppressing business exceptions
            safeCleanup();
        }
    }

    private void enrichMdcFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            TraceContext.setUser(user.getUserId(), user.getEmail());
        }
    }

    private void safeLogEntry(String operation, MethodSignature signature, Object[] args, Traceable traceable) {
        try {
            if (log.isDebugEnabled() && traceable.logInput()) {
                String params = buildParamString(signature, args, traceable.maskFields());
                log.debug(">>> ENTER [{}] args={}", operation, params);
            }
        } catch (Exception e) {
            log.warn("Failed to log method entry for [{}]: {}", operation, e.getMessage());
        }
    }

    private void safeLogExit(String operation, long startNanos, Object result, Traceable traceable) {
        try {
            if (log.isDebugEnabled() && traceable.logOutput()) {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                String output = SensitiveDataMasker.mask(result, traceable.maskFields());
                log.debug("<<< EXIT  [{}] duration={}ms result={}", operation, durationMs, output);
            }
        } catch (Exception e) {
            log.warn("Failed to log method exit for [{}]: {}", operation, e.getMessage());
        }
    }

    private void safeLogError(String operation, long startNanos, Throwable ex) {
        try {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            String rootCause = getRootCauseMessage(ex);
            log.error("!!! ERROR [{}] duration={}ms exception={}: {} rootCause={}",
                    operation, durationMs, ex.getClass().getSimpleName(), ex.getMessage(), rootCause);
        } catch (Exception e) {
            log.warn("Failed to log error for [{}]: {}", operation, e.getMessage());
        }
    }

    private void safeCleanup() {
        try {
            TraceContext.clearOperation();
        } catch (Exception e) {
            log.warn("Failed to clear trace context: {}", e.getMessage());
        }
    }

    private String getRootCauseMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root == ex
                ? ex.getClass().getSimpleName()
                : root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private String buildParamString(MethodSignature signature, Object[] args, String[] maskFields) {
        String[] paramNames = signature.getParameterNames();
        if (paramNames == null || paramNames.length == 0) {
            return "{}";
        }
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (int i = 0; i < paramNames.length; i++) {
            joiner.add(paramNames[i] + "=" + SensitiveDataMasker.mask(args[i], maskFields));
        }
        return joiner.toString();
    }
}
