package com.mario.backend.audit.controller;

import com.mario.backend.audit.dto.AuditLogResponse;
import com.mario.backend.audit.dto.PageResponse;
import com.mario.backend.audit.service.AuditService;
import com.mario.backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('audit:read_all')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AuditLogResponse> auditLogs = auditService.getAuditLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('audit:read_all') or (hasAuthority('audit:read_self') and #userId == authentication.principal.userId)")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AuditLogResponse> auditLogs = auditService.getAuditLogsByUserId(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }
}
