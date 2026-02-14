package com.mario.backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth errors
    EMAIL_EXISTS(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already registered"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired token"),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "TOKEN_BLACKLISTED", "Token has been revoked"),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_TYPE", "Invalid token type"),

    // User errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"),
    INVALID_USER_STATUS(HttpStatus.BAD_REQUEST, "INVALID_USER_STATUS", "Invalid user status"),

    // RBAC errors
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "Role not found"),
    ROLE_NAME_EXISTS(HttpStatus.CONFLICT, "ROLE_NAME_EXISTS", "Role name already exists"),
    ROLE_HAS_USERS(HttpStatus.CONFLICT, "ROLE_HAS_USERS", "Cannot delete role with assigned users"),

    // Face errors
    FACE_REGISTRATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REGISTRATION_FAILED", "Failed to register face"),
    FACE_RECOGNITION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RECOGNITION_FAILED", "Failed to recognize face"),
    FACE_DELETION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "DELETION_FAILED", "Failed to delete face"),
    FACE_ALREADY_REGISTERED(HttpStatus.CONFLICT, "FACE_ALREADY_REGISTERED", "This face image has already been registered"),

    // External service errors
    EXTERNAL_SERVICE_RETRY_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "RETRY_EXHAUSTED", "External service unavailable after retries"),
    EXTERNAL_SERVICE_BAD_RESPONSE(HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_BAD_RESPONSE", "External service returned an error"),

    // Generic
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
