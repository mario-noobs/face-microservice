package com.mario.backend.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt-in annotation for AOP-based method tracing.
 * Annotated methods will have ENTER/EXIT logs at DEBUG level
 * with MDC context (trace_id, user_id, operation).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traceable {

    /**
     * Operation name for MDC and log output.
     * Defaults to {@code ClassName.methodName} if empty.
     */
    String value() default "";

    /**
     * Additional field names to mask beyond the defaults
     * (password, salt, token, imageBase64, etc.).
     */
    String[] maskFields() default {};

    /**
     * Whether to log input parameters. Default true.
     */
    boolean logInput() default true;

    /**
     * Whether to log output result. Default true.
     */
    boolean logOutput() default true;
}
