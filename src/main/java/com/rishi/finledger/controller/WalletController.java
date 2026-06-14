package com.rishi.finledger.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rishi.finledger.Security.CustomUserPrincipal;
import com.rishi.finledger.dto.WalletResponse;
import com.rishi.finledger.service.wallet.WalletService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/wallet")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wallet", description = "Wallet Management APIs")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(summary = "Get wallet by user ID", description = "Returns wallet details for the specified user. Users can access only their own wallet unless they have ADMIN privileges.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wallet fetched successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Wallet not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public WalletResponse getWallet(@PathVariable Long userId, @AuthenticationPrincipal CustomUserPrincipal user) {

        log.info("API HIT | GET /wallet | requester={} | userId={}", user.getUserId(), userId);

        WalletResponse response = walletService.getWalletByUserId(userId);

        log.info("Wallet fetched | userId={} | walletId={}",
                userId,
                response.getWalletId());

        return response;
    }
}
