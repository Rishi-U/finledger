package com.rishi.finledger.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(
        description = "Timestamp of the error",
        example = "2026-06-13T13:03:37")
    private LocalDateTime timestamp;

    @Schema(
        description = "HTTP status code",
        example = "404")
    private int status;

    @Schema(
        description = "HTTP error name",
        example = "NOT_FOUND")
    private String error;

    @Schema(
        description = "Detailed error message",
        example = "Transaction not found")
    private String message;

    @Schema(
        description = "API endpoint path",
        example = "/transactions/1")
    private String path;
}