package com.rishi.finledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Authentication response containing JWT tokens")
public class AuthResponse {

    @Schema(
            description = "JWT access token",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(
            description = "JWT refresh token",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}