package com.rishi.finledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Login request")
public class LoginRequest {
    @Schema(
        description = "User email",
        example = "john@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(
        description = "User password",
        example = "password123")
    @NotBlank(message = "Password is required")
    private String password;
}
