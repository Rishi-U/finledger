package com.rishi.finledger.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Transfer result")
public class TransferResponse {

    @Schema(example = "101")
    private Long transactionId;

    @Schema(example = "SUCCESS")
    private String status;

    @Schema(example = "100.00")
    private BigDecimal amount;

    @Schema(example = "TXN-123456")
    private String referenceId;
}