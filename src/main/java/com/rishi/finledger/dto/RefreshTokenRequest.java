package com.rishi.finledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Refresh token request")
public class RefreshTokenRequest {
    @Schema(
        description = "Refresh token",
        example = "eyJhbGciOiJIUzI1NiJ9...")
    @NotBlank
    private String refreshToken;
}
