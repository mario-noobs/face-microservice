package com.mario.backend.face.controller;

import com.mario.backend.auth.security.AuthenticatedUser;
import com.mario.backend.common.dto.ApiResponse;
import com.mario.backend.face.dto.FaceRecognizeRequest;
import com.mario.backend.face.dto.FaceRegisterRequest;
import com.mario.backend.face.dto.FaceResponse;
import com.mario.backend.face.service.FaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceService faceService;

    @PostMapping("/register-identity")
    @PreAuthorize("hasAuthority('face:register')")
    public ResponseEntity<ApiResponse<FaceResponse>> registerFace(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FaceRegisterRequest request) {
        FaceResponse response = faceService.registerFace(user.getUserId(), request.getImageData());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/recognize-identity")
    @PreAuthorize("hasAuthority('face:recognize')")
    public ResponseEntity<ApiResponse<FaceResponse>> recognizeFace(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FaceRecognizeRequest request) {
        FaceResponse response = faceService.recognizeFace(user.getUserId(), request.getImageData());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/delete-identity")
    @PreAuthorize("hasAuthority('face:delete')")
    public ResponseEntity<ApiResponse<FaceResponse>> deleteFace(
            @AuthenticationPrincipal AuthenticatedUser user) {
        FaceResponse response = faceService.deleteFace(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/is-registered")
    @PreAuthorize("hasAuthority('face:check')")
    public ResponseEntity<ApiResponse<FaceResponse>> isRegistered(
            @AuthenticationPrincipal AuthenticatedUser user) {
        FaceResponse response = faceService.isRegistered(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
