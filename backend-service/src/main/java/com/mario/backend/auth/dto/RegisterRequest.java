package com.mario.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 30, message = "First name must not exceed 30 characters")
    @JsonProperty("first_name")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 30, message = "Last name must not exceed 30 characters")
    @JsonProperty("last_name")
    private String lastName;

    @Valid
    @NotNull(message = "Auth credentials are required")
    @JsonProperty("auth_email_password")
    private AuthEmailPassword authEmailPassword;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthEmailPassword {
        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 30, message = "Password must have from 8 to 30 characters")
        private String password;
    }

    // Helper methods to get email and password
    public String getEmail() {
        return authEmailPassword != null ? authEmailPassword.getEmail() : null;
    }

    public String getPassword() {
        return authEmailPassword != null ? authEmailPassword.getPassword() : null;
    }
}
