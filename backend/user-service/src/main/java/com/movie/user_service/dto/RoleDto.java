package com.movie.user_service.dto;

import lombok.Data;

@Data
public class RoleDto {

    Long userId;
    
    String roleName;

    String reason;
}
