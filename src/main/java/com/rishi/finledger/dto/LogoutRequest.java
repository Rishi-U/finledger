package com.rishi.finledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Logout request")
public class LogoutRequest {
    @Schema(
        description = "Refresh token to revoke",
        example = "eyJhbGciOiJIUzI1NiJ9...")
    @NotBlank
    private String refreshToken;
}