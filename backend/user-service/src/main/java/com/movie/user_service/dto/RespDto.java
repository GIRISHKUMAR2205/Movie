package com.movie.user_service.dto;

import java.util.Set;

import com.movie.user_service.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RespDto {

    @NotNull
    String name;

    @NotNull
    @Email
    String email;

    String token;
    Set<Role> role;
}
