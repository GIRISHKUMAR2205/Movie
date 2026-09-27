package com.movie.user_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDto {

    @NotBlank
    @Size(max = 254)
    @JsonAlias("email")
    private String username;
    
    @NotBlank
    @Size(max = 128)
    private String password;
}
