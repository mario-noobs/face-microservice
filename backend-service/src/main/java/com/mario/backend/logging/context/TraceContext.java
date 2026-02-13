package com.mario.backend.logging.context;

import org.slf4j.MDC;

/**
 * Centralized MDC key management for distributed tracing.
 * All MDC keys used across the application are defined here.
 * <p>
 * Migration to OpenTelemetry: swap MDC calls in this class
 * for OTel Baggage/Span APIs — no other files need to change.
 */
public final class TraceContext {

    public static final String TRACE_ID = "trace_id";
    public static final String USER_ID = "user_id";
    public static final String USER_EMAIL = "user_email";
    public static final String OPERATION = "operation";

    private TraceContext() {}

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setUser(Long userId, String email) {
        if (userId != null) {
            MDC.put(USER_ID, String.valueOf(userId));
        }
        if (email != null) {
            MDC.put(USER_EMAIL, email);
        }
    }

    public static void setOperation(String operation) {
        MDC.put(OPERATION, operation);
    }

    public static void clearOperation() {
        MDC.remove(OPERATION);
    }

    public static void clear() {
        MDC.clear();
    }
}
