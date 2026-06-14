package com.rishi.finledger.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Transaction details")
public class TransactionResponse {

    @Schema(example = "1")
    private Long transactionId;

    @Schema(example = "2")
    private Long senderUserId;

    @Schema(example = "3")
    private Long receiverUserId;

    @Schema(example = "500.00")
    private BigDecimal amount;

    @Schema(
        description = "Transaction status",
        example = "SUCCESS")
    private TransactionStatus status;

    @Schema(
        description = "Transaction type",
        example = "TRANSFER")
    private TransactionType type;

    @Schema(example = "2026-06-13T13:15:30")
    private LocalDateTime createdAt;
}