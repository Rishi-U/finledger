package com.rishi.finledger.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Wallet details")
public class WalletResponse {

    @Schema(
        description = "Wallet ID",
        example = "10")
    private Long walletId;

    @Schema(
        description = "Current wallet balance",
        example = "5000.00")
    private BigDecimal balance;
}