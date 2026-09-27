package com.movie.user_service.dto;

import java.time.LocalDateTime;

import com.movie.user_service.entity.RequestStatus;
import com.movie.user_service.entity.RoleStatus;

public record RoleRequestSummaryDto(
        Long requestId,
        String userName,
        String userEmail,
        String requestedRole,
        RequestStatus requestStatus,
        RoleStatus roleStatus,
        LocalDateTime requestedAt) {
}
