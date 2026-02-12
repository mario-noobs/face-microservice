package com.mario.backend.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 30, message = "First name must not exceed 30 characters")
    @JsonProperty("first_name")
    private String firstName;

    @Size(max = 30, message = "Last name must not exceed 30 characters")
    @JsonProperty("last_name")
    private String lastName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
}
