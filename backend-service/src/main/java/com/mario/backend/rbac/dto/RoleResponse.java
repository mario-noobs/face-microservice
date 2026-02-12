package com.mario.backend.rbac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    private Long id;
    private String name;
    private String description;

    @JsonProperty("is_default")
    private Boolean isDefault;

    private List<PermissionResponse> permissions;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
