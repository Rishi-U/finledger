/**
 * REFUND LOGIC:
 *
 * - Refund is a NEW transaction (ledger reversal), not an update/delete.
 * - Only TRANSFER transactions can be refunded.
 * - Supports PARTIAL refunds.
 * - Only SUCCESS refunds are considered for remaining calculation.
 * - Refund does NOT include fee reversal (business decision).
 *
 * Ledger effect:
 *   Original:  A → B
 *   Refund:    B → A (only amount, not fee)
 *
 * Idempotency:
 *   - referenceId is used to prevent duplicate refunds.
 *
 * Concurrency:
 *   - Uses pessimistic locking on transaction + wallets.
 */

package com.rishi.finledger.service.wallet;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import com.rishi.finledger.exception.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rishi.finledger.dto.FraudCheckResult;
import com.rishi.finledger.dto.RefundRequest;
import com.rishi.finledger.dto.TransferResponse;
import com.rishi.finledger.entity.EntryType;
import com.rishi.finledger.entity.LedgerEntryEntity;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;
import com.rishi.finledger.entity.WalletEntity;
import com.rishi.finledger.event.RefundCompletedEvent;
import com.rishi.finledger.exception.InsufficientBalanceException;
import com.rishi.finledger.exception.InvalidTransactionException;
import com.rishi.finledger.mapper.LedgerEntryMapper;
import com.rishi.finledger.mapper.TransactionMapper;
import com.rishi.finledger.repository.LedgerRepository;
import com.rishi.finledger.repository.TransactionRepository;
import com.rishi.finledger.repository.WalletRepository;
import com.rishi.finledger.util.TransactionFinalStateResolver;

@Service
public class RefundService {
    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final WalletRepository walletRepository;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final LedgerRepository ledgerRepository;
    private final TransactionFinalStateResolver transactionFinalStateResolver;
    private final FraudService fraudService;
    private final ApplicationEventPublisher publisher;

    public RefundService(TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            WalletRepository walletRepository,
            LedgerEntryMapper ledgerEntryMapper,
            LedgerRepository ledgerRepository,
            TransactionFinalStateResolver transactionFinalStateResolver,
            FraudService fraudService,
            ApplicationEventPublisher publisher) {

        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.walletRepository = walletRepository;
        this.ledgerEntryMapper = ledgerEntryMapper;
        this.ledgerRepository = ledgerRepository;
        this.transactionFinalStateResolver = transactionFinalStateResolver;
        this.fraudService = fraudService;
        this.publisher = publisher;
    }

    @Transactional
    public TransferResponse refundTransaction(Long transactionId, RefundRequest request, Long currentUserId) {

        log.info("Refund START | originalTxId={} | amount={} | refId={}",
                transactionId, request.getAmount(), request.getReferenceId());

        // ✅ STEP 0: IDEMPOTENCY
        TransactionEntity existingTx = transactionRepository.findByReferenceId(request.getReferenceId()).orElse(null);

        if (existingTx != null) {
            log.warn("Duplicate request detected (idempotent) | refId={}", request.getReferenceId());
            return transactionMapper.toTransferResponse(existingTx);
        }

        // ✅ STEP 1: FETCH ORIGINAL
        TransactionEntity originalTx = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found"));

        Long refundOwnerId = originalTx.getReceiver().getUser().getId();

        if (!refundOwnerId.equals(currentUserId)) {
            log.warn("Unauthorized refund attempt | userId={} | originalTxId={}", currentUserId, transactionId);
            throw new AuthorizationDeniedException("You are not allowed to refund this transaction");
        }

        if (originalTx.getStatus() != TransactionStatus.SUCCESS) {
            throw new InvalidTransactionException("Only SUCCESS transactions can be refunded");
        }

        if (originalTx.getType() != TransactionType.TRANSFER) {
            throw new InvalidTransactionException("Only TRANSFER transactions can be refunded");
        }

        if (originalTx.getSender().getId().equals(originalTx.getReceiver().getId())) {
            throw new InvalidTransactionException("Invalid transaction for refund");
        }

        BigDecimal refundAmount = request.getAmount();

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Invalid refund amount");
        }

        // ✅ STEP 2: CALCULATE TOTAL REFUNDED SO FAR
        List<TransactionEntity> previousRefunds = transactionRepository.findByOriginalTransactionIdAndType(
                transactionId,
                TransactionType.REFUND);

        BigDecimal totalRefund = previousRefunds.stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.SUCCESS)
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingAmount = originalTx.getAmount().subtract(totalRefund);

        if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidTransactionException("Transaction already fully refunded");
        }

        if (refundAmount.compareTo(remainingAmount) > 0) {
            throw new InvalidTransactionException("Refund exceeds remaining amount");
        }

        // ✅ STEP 3: LOCK WALLETS

        WalletEntity originalSender = walletRepository
                .findByIdForUpdate(originalTx.getSender().getId())
                .orElseThrow();

        WalletEntity originalReceiver = walletRepository
                .findByIdForUpdate(originalTx.getReceiver().getId())
                .orElseThrow();

        WalletEntity refundFrom = originalReceiver; // money goes OUT
        WalletEntity refundTo = originalSender; // money comes IN

        if (refundFrom.getBalance().compareTo(refundAmount) < 0) {
            throw new InsufficientBalanceException("Receiver has insufficient balance for refund");
        }

        FraudCheckResult fraud = fraudService.checkRefundFraud(
                transactionId,
                refundFrom.getUser().getId(),
                refundAmount,
                originalTx.getAmount());

        if (fraud.isBlocked()) {
            throw new InvalidTransactionException(fraud.getReason());
        }
        TransactionEntity refundTx = transactionMapper.toRefundEntity(
                request,
                refundTo,
                refundFrom,
                TransactionStatus.PENDING,
                TransactionType.REFUND,
                transactionId,
                fraud);

        try {
            transactionRepository.save(refundTx);
        } catch (DataIntegrityViolationException e) {
            TransactionEntity existing = transactionFinalStateResolver.waitForFinalState(request.getReferenceId());

            log.warn("Duplicate transaction (race) | refId={} | originalTxId={}",
                    existing.getReferenceId(),
                    existing.getOriginalTransactionId());

            return transactionMapper.toTransferResponse(existing);
        }

        try {
            // ✅ STEP 5: LEDGER (REVERSE)

            LedgerEntryEntity debit = ledgerEntryMapper.toEntity(
                    refundTx, refundFrom, refundAmount, EntryType.DEBIT);

            LedgerEntryEntity credit = ledgerEntryMapper.toEntity(
                    refundTx, refundTo, refundAmount, EntryType.CREDIT);

            if (debit.getAmount().compareTo(credit.getAmount()) != 0) {
                log.error("Ledger imbalance | refId={} | debit={} | credit={}",
                        request.getReferenceId(),
                        debit.getAmount(),
                        credit.getAmount());

                throw new InvalidTransactionException("Ledger imbalance detected");
            }

            ledgerRepository.saveAll(List.of(debit, credit));

            // ✅ STEP 6: UPDATE BALANCES

            refundFrom.setBalance(refundFrom.getBalance().subtract(refundAmount));
            refundTo.setBalance(refundTo.getBalance().add(refundAmount));

            walletRepository.saveAll(List.of(refundFrom, refundTo));

            // ✅ STEP 7: FINALIZE
            refundTx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(refundTx);

            log.info("REFUND SUCCESS | refundTxId={} | amount={} | originalTxId={}",
                    refundTx.getId(),
                    refundAmount,
                    transactionId);

            publisher.publishEvent(
                    new RefundCompletedEvent(
                            refundTx.getId(),
                            transactionId,
                            refundTx.getReferenceId(),
                            refundAmount));

            return transactionMapper.toTransferResponse(refundTx);

        } catch (Exception e) {
            log.error("REFUND FAILED | refId={} | error={}",
                    request.getReferenceId(),
                    e.getMessage(),
                    e);

            refundTx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(refundTx);

            throw e;
        }

    }

}
