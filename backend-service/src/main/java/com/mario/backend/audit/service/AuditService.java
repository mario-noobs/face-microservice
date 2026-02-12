package com.mario.backend.audit.service;

import com.mario.backend.audit.dto.AuditLogResponse;
import com.mario.backend.audit.dto.PageResponse;
import com.mario.backend.audit.entity.AuditLog;
import com.mario.backend.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void saveAuditLog(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    public PageResponse<AuditLogResponse> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditLogs = auditLogRepository.findAll(pageable);

        return buildPageResponse(auditLogs);
    }

    public PageResponse<AuditLogResponse> getAuditLogsByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditLogs = auditLogRepository.findByUserId(userId, pageable);

        return buildPageResponse(auditLogs);
    }

    private PageResponse<AuditLogResponse> buildPageResponse(Page<AuditLog> page) {
        List<AuditLogResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<AuditLogResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .requestId(auditLog.getRequestId())
                .userId(auditLog.getUserId())
                .method(auditLog.getMethod())
                .path(auditLog.getPath())
                .statusCode(auditLog.getStatusCode())
                .clientIp(auditLog.getClientIp())
                .userAgent(auditLog.getUserAgent())
                .durationMs(auditLog.getDurationMs())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
