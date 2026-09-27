package com.movie.user_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.movie.user_service.entity.RequestStatus;
import com.movie.user_service.entity.RoleStatus;

public record RoleRequestProgressDto(
        Long requestId,
        String requestedRole,
        RequestStatus requestStatus,
        RoleStatus roleStatus,
        LocalDateTime requestedAt,
        List<RoleAuditStatusDto> auditTrail) {
}
