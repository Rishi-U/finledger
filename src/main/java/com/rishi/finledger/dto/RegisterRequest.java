package com.rishi.finledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "User registration request")
public class RegisterRequest {
    @Schema(
        description = "Full name",
        example = "Rishi U")
    @NotBlank(message = "Name is Required")
    private String name;

    @Schema(
        description = "Email address",
        example = "rishi@gmail.com")
    @NotBlank(message =  "Email is Required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(
        description = "Password",
        example = "password123")
    @NotBlank(message = "Password is Required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
