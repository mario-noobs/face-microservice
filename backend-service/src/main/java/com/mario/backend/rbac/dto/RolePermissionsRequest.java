package com.mario.backend.rbac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionsRequest {

    @NotNull(message = "Permission IDs are required")
    @JsonProperty("permission_ids")
    private List<Long> permissionIds;
}
