package com.mario.backend.users.controller;

import com.mario.backend.auth.security.AuthenticatedUser;
import com.mario.backend.common.dto.ApiResponse;
import com.mario.backend.users.dto.UpdateProfileRequest;
import com.mario.backend.users.dto.UserResponse;
import com.mario.backend.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('user:read_self')")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal AuthenticatedUser user) {
        UserResponse profile = userService.getProfile(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('user:update_self')")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse profile = userService.updateProfile(user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
