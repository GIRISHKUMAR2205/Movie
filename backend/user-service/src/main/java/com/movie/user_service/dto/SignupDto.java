package com.movie.user_service.dto;

import lombok.Data;

@Data
public class SignupDto {
    String name;
    String email;
    String role;
    String password;
    String confirmPassword;
}
