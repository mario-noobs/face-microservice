/**
 * Distributed Logging Module.
 * <p>
 * Provides AOP-based method tracing ({@code @Traceable}), MDC-based
 * trace context propagation ({@link com.mario.backend.logging.context.TraceContext}),
 * and sensitive data masking for log output.
 * <p>
 * <b>OpenTelemetry migration path:</b> Replace MDC calls in
 * {@code TraceContext} with OTel Baggage/Span APIs. All other
 * classes reference {@code TraceContext} exclusively, so no
 * further changes are needed.
 */
package com.mario.backend.logging;
