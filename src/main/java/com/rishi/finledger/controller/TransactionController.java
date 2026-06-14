package com.rishi.finledger.controller;

import com.rishi.finledger.Security.CustomUserPrincipal;
import com.rishi.finledger.dto.*;
import com.rishi.finledger.service.wallet.TransactionHistoryService;
import com.rishi.finledger.service.wallet.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.rishi.finledger.service.wallet.RefundService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Tag(name = "Transactions", description = "Money transfer, refunds and transaction history operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final TransactionHistoryService transactionHistoryService;
    private final RefundService refundService;

    public TransactionController(
            TransactionService transactionService,
            TransactionHistoryService transactionHistoryService,
            RefundService refundService) {

        this.transactionService = transactionService;
        this.transactionHistoryService = transactionHistoryService;
        this.refundService = refundService;
    }

    /*
     * MONEY TRANSFER
     */
    @Operation(summary = "Transfer money", description = "Transfers money from the authenticated user's wallet to another user's wallet")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer successful", content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Receiver wallet not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate reference ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/transfer")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        request.setSenderUserId(user.getUserId());
        log.info("API HIT | POST /transactions/transfer | refId={} | senderId={} | receiverId={}",
                request.getReferenceId(),
                user.getUserId(),
                request.getReceiverUserId());

        TransferResponse response = transactionService.transferMoney(request);

        log.info("TRANSFER SUCCESS | refId={} | status={}",
                response.getReferenceId(),
                response.getStatus());

        return response;
    }

    /*
     * REFUND TRANSACTION (PARTIAL / FULL)
     *
     * - Creates a NEW transaction (ledger reversal)
     * - Supports partial refund
     * - Fees are NOT refunded (business rule)
     */
    @Operation(summary = "Refund transaction", description = """
            Creates a new refund transaction.
            Supports both full and partial refunds.
            Fees are not refunded.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund successful"),
            @ApiResponse(responseCode = "400", description = "Invalid refund request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "409", description = "Refund already processed")
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/refund")
    public ResponseEntity<TransferResponse> refund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        log.info("API HIT | POST /transactions/{}/refund | userId={} | refId={} | amount={}",
                id,
                user.getUserId(),
                request.getReferenceId(),
                request.getAmount());

        TransferResponse response = refundService.refundTransaction(
                id,
                request,
                user.getUserId());

        log.info("REFUND COMPLETED | refId={} | status={}",
                response.getReferenceId(),
                response.getStatus());

        return ResponseEntity.ok(response);
    }

    /*
     * GET USER TRANSACTIONS
     */
    @Operation(summary = "Get transaction history", description = "Returns all transactions belonging to the specified user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public List<TransactionResponse> getTransactions(@PathVariable Long userId) {

        log.info("API HIT | GET /transactions/{} | userId={}", userId, userId);

        List<TransactionResponse> response = transactionHistoryService.getTransactionsByUserId(userId);

        log.info("TRANSACTION HISTORY FETCHED | userId={} | count={}",
                userId,
                response.size());

        return response;
    }
}