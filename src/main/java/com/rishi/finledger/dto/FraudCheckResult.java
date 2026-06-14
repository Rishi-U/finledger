package com.rishi.finledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Fraud detection result")
public class FraudCheckResult {

    @Schema(
        description = "Whether transaction is blocked",
        example = "false")
    private boolean blocked;

    @Schema(
        description = "Whether transaction is flagged for review",
        example = "true")
    private boolean flagged;

    @Schema(
        description = "Reason for fraud decision",
        example = "High amount transaction")
    private String reason;
}