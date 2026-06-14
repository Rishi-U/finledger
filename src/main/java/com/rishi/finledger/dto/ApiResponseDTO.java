package com.rishi.finledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Standard API response wrapper")
public class ApiResponseDTO<T> {

    @Schema(
            description = "Response message",
            example = "Transfer completed successfully")
    private String message;

    @Schema(description = "Response payload")
    private T data;
}
