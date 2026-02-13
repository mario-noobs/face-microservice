package com.mario.backend.users.service;

import com.mario.backend.common.exception.ApiException;
import com.mario.backend.common.exception.ErrorCode;
import com.mario.backend.logging.annotation.Traceable;
import com.mario.backend.rbac.entity.Permission;
import com.mario.backend.rbac.entity.Role;
import com.mario.backend.users.dto.UpdateProfileRequest;
import com.mario.backend.users.dto.UserResponse;
import com.mario.backend.users.entity.User;
import com.mario.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Traceable("user.getProfile")
    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return mapToResponse(user);
    }

    @Traceable("user.updateProfile")
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }

        user = userRepository.save(user);
        return mapToResponse(user);
    }

    @Traceable("user.updateStatus")
    @Transactional
    public UserResponse updateStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        try {
            user.setStatus(User.UserStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_USER_STATUS, "Invalid status: " + status + ". Must be one of: activated, deactivated, banned");
        }

        user = userRepository.save(user);
        return mapToResponse(user);
    }

    public UserResponse mapToResponse(User user) {
        Role role = user.getRole();
        UserResponse.RoleInfo roleInfo = null;
        if (role != null) {
            List<String> permissions = role.getPermissions().stream()
                    .map(Permission::getName)
                    .toList();
            roleInfo = UserResponse.RoleInfo.builder()
                    .name(role.getName())
                    .permissions(permissions)
                    .build();
        }

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .role(roleInfo)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
