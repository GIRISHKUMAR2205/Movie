package com.movie.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RespDto {
    String name;
    String email;
    String token;
    String role;
}
