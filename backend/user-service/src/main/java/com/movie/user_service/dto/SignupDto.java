package com.movie.user_service.dto;

import com.movie.user_service.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignupDto {
    @NotNull
    String name;
    @NotNull
        @Email
    String email;
    Role role;
        @NotNull
    String password;
    @NotNull
    String confirmPassword;
}
