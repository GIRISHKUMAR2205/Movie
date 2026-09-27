package com.movie.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleDto {
    
    @NotBlank
    @Size(max = 100)
    private String roleName;

    @Size(max = 500)
    private String reason;
}
