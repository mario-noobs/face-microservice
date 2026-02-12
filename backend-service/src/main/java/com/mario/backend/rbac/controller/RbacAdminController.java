package com.mario.backend.rbac.controller;

import com.mario.backend.common.dto.ApiResponse;
import com.mario.backend.rbac.dto.*;
import com.mario.backend.rbac.service.RbacService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/rbac")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class RbacAdminController {

    private final RbacService rbacService;

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(rbacService.getAllRoles()));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody RoleCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.createRole(request)));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.updateRole(id, request)));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions(
            @RequestParam(required = false) String service) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.getAllPermissions(service)));
    }

    @PutMapping("/roles/{id}/permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> setRolePermissions(
            @PathVariable Long id,
            @Valid @RequestBody RolePermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.setRolePermissions(id, request)));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUser(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {
        rbacService.assignRoleToUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
