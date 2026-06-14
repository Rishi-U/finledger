package com.rishi.finledger.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Money transfer request")
public class TransferRequest {
    
    @Schema(
        description = "Sender user ID",
        example = "2")
    @NotNull
    private Long senderUserId;

    @Schema(
        description = "Receiver user ID",
        example = "3")
    @NotNull
    private Long receiverUserId;

    @Schema(
        description = "Transfer amount",
        example = "100.00")
    @NotNull
    @DecimalMin(value =  "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Schema(
        description = "Unique transfer reference",
        example = "TXN-123456")
    @NotBlank
    private String referenceId;
}

