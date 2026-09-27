package com.movie.user_service.dto;

import java.time.LocalDateTime;

import com.movie.user_service.entity.RequestStatus;

/** A user-safe audit entry for the progress of their own role request. */
public record RoleAuditStatusDto(
        RequestStatus previousStatus,
        RequestStatus currentStatus,
        String reason,
        LocalDateTime changedAt) {
}
