package com.mario.backend.rbac.service;

import com.mario.backend.common.exception.ApiException;
import com.mario.backend.common.exception.ErrorCode;
import com.mario.backend.logging.annotation.Traceable;
import com.mario.backend.rbac.dto.*;
import com.mario.backend.rbac.entity.Permission;
import com.mario.backend.rbac.entity.Role;
import com.mario.backend.rbac.repository.PermissionRepository;
import com.mario.backend.rbac.repository.RoleRepository;
import com.mario.backend.users.entity.User;
import com.mario.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    // --- Role operations ---

    @Traceable("rbac.getAllRoles")
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapRoleToResponse)
                .toList();
    }

    @Traceable("rbac.getRoleById")
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND));
        return mapRoleToResponse(role);
    }

    @Traceable("rbac.createRole")
    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new ApiException(ErrorCode.ROLE_NAME_EXISTS);
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
        }

        // If setting as default, clear other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultRoles();
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .permissions(permissions)
                .build();

        role = roleRepository.save(role);
        return mapRoleToResponse(role);
    }

    @Traceable("rbac.updateRole")
    @Transactional
    public RoleResponse updateRole(Long id, RoleUpdateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND));

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByName(request.getName())) {
                throw new ApiException(ErrorCode.ROLE_NAME_EXISTS);
            }
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        if (request.getIsDefault() != null) {
            if (Boolean.TRUE.equals(request.getIsDefault())) {
                clearDefaultRoles();
            }
            role.setIsDefault(request.getIsDefault());
        }

        role = roleRepository.save(role);
        return mapRoleToResponse(role);
    }

    @Traceable("rbac.deleteRole")
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND));

        long usersWithRole = userRepository.countByRoleName(role.getName());
        if (usersWithRole > 0) {
            throw new ApiException(ErrorCode.ROLE_HAS_USERS, "Cannot delete role with " + usersWithRole + " assigned users");
        }

        roleRepository.delete(role);
    }

    // --- Permission operations ---

    @Traceable("rbac.getAllPermissions")
    public List<PermissionResponse> getAllPermissions(String service) {
        List<Permission> permissions;
        if (StringUtils.hasText(service)) {
            permissions = permissionRepository.findByService(service);
        } else {
            permissions = permissionRepository.findAll();
        }
        return permissions.stream()
                .map(this::mapPermissionToResponse)
                .toList();
    }

    @Traceable("rbac.setRolePermissions")
    @Transactional
    public RoleResponse setRolePermissions(Long roleId, RolePermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND));

        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
        role.setPermissions(permissions);

        role = roleRepository.save(role);
        return mapRoleToResponse(role);
    }

    // --- User role assignment ---

    @Traceable("rbac.assignRoleToUser")
    @Transactional
    public void assignRoleToUser(Long userId, AssignRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + request.getRoleName()));

        user.setRole(role);
        userRepository.save(user);
    }

    public Role getDefaultRole() {
        return roleRepository.findByIsDefaultTrue()
                .orElseGet(() -> roleRepository.findByName("BASIC_USER").orElse(null));
    }

    // --- Helpers ---

    private void clearDefaultRoles() {
        roleRepository.findByIsDefaultTrue().ifPresent(existing -> {
            existing.setIsDefault(false);
            roleRepository.save(existing);
        });
    }

    private RoleResponse mapRoleToResponse(Role role) {
        List<PermissionResponse> permissionResponses = role.getPermissions().stream()
                .map(this::mapPermissionToResponse)
                .collect(Collectors.toList());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isDefault(role.getIsDefault())
                .permissions(permissionResponses)
                .createdAt(role.getCreatedAt())
                .build();
    }

    private PermissionResponse mapPermissionToResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .service(permission.getService())
                .build();
    }
}
