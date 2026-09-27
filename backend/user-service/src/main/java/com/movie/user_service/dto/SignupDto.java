package com.movie.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupDto {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(min = 12, max = 128)
    private String password;

    @NotBlank
    @Size(max = 128)
    private String confirmPassword;

    @AssertTrue(message = "Password confirmation must match the password")
    public boolean isPasswordConfirmationMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
