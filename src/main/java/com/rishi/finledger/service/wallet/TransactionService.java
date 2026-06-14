package com.rishi.finledger.service.wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rishi.finledger.dto.FraudCheckResult;
import com.rishi.finledger.dto.TransferRequest;
import com.rishi.finledger.dto.TransferResponse;
import com.rishi.finledger.entity.*;
import com.rishi.finledger.event.TransferCompletedEvent;
import com.rishi.finledger.exception.InsufficientBalanceException;
import com.rishi.finledger.exception.InvalidTransactionException;
import com.rishi.finledger.exception.WalletNotFoundException;
import com.rishi.finledger.mapper.LedgerEntryMapper;
import com.rishi.finledger.mapper.TransactionMapper;
import com.rishi.finledger.repository.*;
import com.rishi.finledger.service.auth.RateLimiterService;
import com.rishi.finledger.util.SystemWalletProvider;
import com.rishi.finledger.util.TransactionFinalStateResolver;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final LedgerRepository ledgerRepository;
    private final RateLimiterService rateLimiterService;
    private final FeeService feeService;
    private final SystemWalletProvider systemWalletProvider;
    private final TransactionFinalStateResolver transactionFinalStateResolver;
    private final FraudService fraudService;
    private final ApplicationEventPublisher publisher;

    public TransactionService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            LedgerEntryMapper ledgerEntryMapper,
            LedgerRepository ledgerRepository,
            RateLimiterService rateLimiterService,
            FeeService feeService,
            SystemWalletProvider systemWalletProvider,
            TransactionFinalStateResolver transactionFinalStateResolver,
            FraudService fraudService,
            ApplicationEventPublisher publisher) {

        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.ledgerEntryMapper = ledgerEntryMapper;
        this.ledgerRepository = ledgerRepository;
        this.rateLimiterService = rateLimiterService;
        this.feeService = feeService;
        this.systemWalletProvider = systemWalletProvider;
        this.transactionFinalStateResolver = transactionFinalStateResolver;
        this.fraudService = fraudService;
        this.publisher = publisher;
    }

    @Transactional
    public TransferResponse transferMoney(TransferRequest request) {

        log.info("Transfer START | refId={} | senderId={} | receiverId={} | amount={}",
                request.getReferenceId(),
                request.getSenderUserId(),
                request.getReceiverUserId(),
                request.getAmount());

        // ✅ STEP 0: IDEMPOTENCY FIRST (CRITICAL)
        TransactionEntity existingTx = transactionRepository
                .findByReferenceId(request.getReferenceId())
                .orElse(null);

        if (existingTx != null) {

            log.info("Idempotent request | refId={} | status={}",
                    existingTx.getReferenceId(),
                    existingTx.getStatus());

            return transactionMapper.toTransferResponse(existingTx);
        }

        // ✅ STEP 1: RATE LIMIT AFTER IDEMPOTENCY
        rateLimiterService.checkRateLimit(request.getSenderUserId());

        // ✅ STEP 2: VALIDATION
        if (request.getAmount() == null ||
                request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransactionException("Amount must be greater than zero");
        }

        if (request.getSenderUserId().equals(request.getReceiverUserId())) {
            throw new InvalidTransactionException("Cannot transfer to same user");
        }

        FraudCheckResult fraud = fraudService.checkTransferFraud(
                request.getSenderUserId(),
                request.getAmount());

        if (fraud.isBlocked()) {
            log.warn("TRANSFER BLOCKED | reason={}", fraud.getReason());

            throw new InvalidTransactionException(fraud.getReason());
        }

        // ✅ STEP 3: LOCK WALLETS
        WalletEntity senderWallet = walletRepository
                .findByUserIdForUpdate(request.getSenderUserId())
                .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));

        WalletEntity receiverWallet = walletRepository
                .findByUserIdForUpdate(request.getReceiverUserId())
                .orElseThrow(() -> new WalletNotFoundException("Receiver wallet not found"));

        WalletEntity systemWallet = walletRepository
                .findByIdForUpdate(systemWalletProvider.getSystemWalletId())
                .orElseThrow(() -> new WalletNotFoundException("System wallet not found"));

        // ✅ STEP 4: CREATE TRANSACTION
        TransactionEntity tx = transactionMapper.toEntity(
                request,
                senderWallet,
                receiverWallet,
                TransactionStatus.PENDING,
                TransactionType.TRANSFER,
                fraud);

        try {
            transactionRepository.save(tx);
        } catch (DataIntegrityViolationException e) {

            // 🔁 fallback idempotency (race condition safe)
            TransactionEntity existing = transactionFinalStateResolver.waitForFinalState(request.getReferenceId());

            log.warn("Duplicate transaction (race) | refId={} | txId={}",
                    existing.getReferenceId(),
                    existing.getId());

            return transactionMapper.toTransferResponse(existing);
        }

        // ✅ STEP 5: CALCULATE FEE
        BigDecimal fee = feeService.calculateFee(request.getAmount());
        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Invalid fee calculated");
        }
        BigDecimal totalDebit = request.getAmount().add(fee);

        try {

            // STEP 6: BALANCE CHECK
            if (senderWallet.getBalance().compareTo(totalDebit) < 0) {

                tx.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(tx);

                log.warn("Transaction FAILED | refId={} | reason=INSUFFICIENT_BALANCE | required={} | available={}",
                        request.getReferenceId(),
                        totalDebit,
                        senderWallet.getBalance());

                throw new InsufficientBalanceException("Insufficient balance (including fee)");
            }

            // STEP 7: CREATE LEDGER
            LedgerEntryEntity debitEntry = ledgerEntryMapper.toEntity(
                    tx, senderWallet, totalDebit, EntryType.DEBIT);

            LedgerEntryEntity creditEntry = ledgerEntryMapper.toEntity(
                    tx, receiverWallet, request.getAmount(), EntryType.CREDIT);

            List<LedgerEntryEntity> entries = new ArrayList<>();

            entries.add(debitEntry);
            entries.add(creditEntry);

            // 🔥 Only add system entry if fee > 0
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                LedgerEntryEntity systemCredit = ledgerEntryMapper.toEntity(
                        tx, systemWallet, fee, EntryType.CREDIT);

                entries.add(systemCredit);
            }

            // ✅ STEP 8: VALIDATE LEDGER
            BigDecimal sumDebit = debitEntry.getAmount();
            BigDecimal sumCredit = creditEntry.getAmount();

            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                sumCredit = sumCredit.add(fee);
            }

            if (sumDebit.compareTo(sumCredit) != 0) {
                log.error("Ledger imbalance | refId={} | debit={} | credit={}",
                        request.getReferenceId(),
                        sumDebit,
                        sumCredit);

                throw new InvalidTransactionException("Ledger imbalance detected");
            }

            // STEP 9 → SAVE LEDGER
            ledgerRepository.saveAll(entries);

            // STEP 10 → UPDATE WALLET (CACHE)
            senderWallet.setBalance(senderWallet.getBalance().subtract(totalDebit));
            receiverWallet.setBalance(receiverWallet.getBalance().add(request.getAmount()));

            List<WalletEntity> walletsToSave = new ArrayList<>();
            walletsToSave.add(senderWallet);
            walletsToSave.add(receiverWallet);

            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                systemWallet.setBalance(systemWallet.getBalance().add(fee));
                walletsToSave.add(systemWallet);
            }

            walletRepository.saveAll(walletsToSave);

            // STEP 11 → FINALIZE TX
            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);

            log.info("Transfer SUCCESS | refId={} | txId={} | amount={} | fee={}",
                    tx.getReferenceId(),
                    tx.getId(),
                    request.getAmount(),
                    fee);

            publisher.publishEvent(
                    new TransferCompletedEvent(
                            tx.getId(),
                            tx.getReferenceId(),
                            senderWallet.getUser().getId(),
                            receiverWallet.getUser().getId(),
                            tx.getAmount()));

            return transactionMapper.toTransferResponse(tx);

        } catch (Exception e) {

            log.error("Transaction FAILED (SYSTEM) | refId={} | error={}",
                    request.getReferenceId(),
                    e.getMessage(),
                    e);

            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);

            throw e;
        }
    }
}
