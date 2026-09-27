package com.movie.user_service.dto;

import jakarta.validation.constraints.Size;

public record RoleDecisionDto(@Size(max = 500) String reason) {
}
