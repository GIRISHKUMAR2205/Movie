package com.movie.user_service.dto;

import java.time.LocalDateTime;

import com.movie.user_service.entity.RequestStatus;

public record RoleAuditDto(
        Long auditId,
        RequestStatus previousStatus,
        RequestStatus currentStatus,
        String reason,
        String changedBy,
        LocalDateTime changedAt) {
}
