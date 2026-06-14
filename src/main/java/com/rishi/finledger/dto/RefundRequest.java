package com.rishi.finledger.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Refund request")
public class RefundRequest {
    @Schema(
        description = "Refund amount",
        example = "50.00")
    @NotNull
    @DecimalMin(value =  "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Schema(
        description = "Unique refund reference",
        example = "RFN-001")
    @NotBlank
    private String referenceId;
}
