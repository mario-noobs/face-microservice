package com.mario.backend.users.controller;

import com.mario.backend.audit.dto.PageResponse;
import com.mario.backend.common.dto.ApiResponse;
import com.mario.backend.common.exception.ApiException;
import com.mario.backend.users.dto.UpdateProfileRequest;
import com.mario.backend.users.dto.UserResponse;
import com.mario.backend.users.entity.User;
import com.mario.backend.users.repository.UserRepository;
import com.mario.backend.users.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('user:list')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> userPage = userRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<UserResponse> content = userPage.getContent().stream()
                .map(userService::mapToResponse)
                .toList();

        PageResponse<UserResponse> response = PageResponse.<UserResponse>builder()
                .content(content)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read_any')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        UserResponse profile = userService.getProfile(id);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update_any')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse profile = userService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:update_any')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        UserResponse profile = userService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotBlank(message = "Status is required")
        private String status;
    }
}
