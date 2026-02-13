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

        String operation = traceable.value().isEmpty()
                ? signature.getDeclaringType().getSimpleName() + "." + signature.getName()
                : traceable.value();

        enrichMdcFromSecurityContext();
        TraceContext.setOperation(operation);

        long startNanos = System.nanoTime();
        try {
            if (log.isDebugEnabled() && traceable.logInput()) {
                String params = buildParamString(signature, joinPoint.getArgs(), traceable.maskFields());
                log.debug(">>> ENTER [{}] args={}", operation, params);
            }

            Object result = joinPoint.proceed();

            if (log.isDebugEnabled() && traceable.logOutput()) {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                String output = SensitiveDataMasker.mask(result, traceable.maskFields());
                log.debug("<<< EXIT  [{}] duration={}ms result={}", operation, durationMs, output);
            }

            return result;
        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            String rootCause = getRootCauseMessage(ex);
            log.error("!!! ERROR [{}] duration={}ms exception={}: {} rootCause={}",
                    operation, durationMs, ex.getClass().getSimpleName(), ex.getMessage(), rootCause);
            throw ex;
        } finally {
            TraceContext.clearOperation();
        }
    }

    private void enrichMdcFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            TraceContext.setUser(user.getUserId(), user.getEmail());
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
