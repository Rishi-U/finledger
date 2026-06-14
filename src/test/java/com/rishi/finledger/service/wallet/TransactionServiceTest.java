package com.rishi.finledger.service.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.rishi.finledger.dto.FraudCheckResult;
import com.rishi.finledger.dto.TransferRequest;
import com.rishi.finledger.dto.TransferResponse;
import com.rishi.finledger.entity.EntryType;
import com.rishi.finledger.entity.LedgerEntryEntity;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;
import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.entity.WalletEntity;
import com.rishi.finledger.event.TransferCompletedEvent;
import com.rishi.finledger.exception.InsufficientBalanceException;
import com.rishi.finledger.exception.InvalidTransactionException;
import com.rishi.finledger.mapper.LedgerEntryMapper;
import com.rishi.finledger.mapper.TransactionMapper;
import com.rishi.finledger.repository.LedgerRepository;
import com.rishi.finledger.repository.TransactionRepository;
import com.rishi.finledger.repository.WalletRepository;
import com.rishi.finledger.service.auth.RateLimiterService;
import com.rishi.finledger.util.SystemWalletProvider;
import com.rishi.finledger.util.TransactionFinalStateResolver;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private FeeService feeService;

    @Mock
    private SystemWalletProvider systemWalletProvider;

    @Mock
    private TransactionFinalStateResolver transactionFinalStateResolver;

    @Mock
    private FraudService fraudService;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private TransactionService transactionService;

    private TransferRequest request;
    private WalletEntity senderWallet;
    private WalletEntity receiverWallet;
    private WalletEntity systemWallet;
    private TransactionEntity transaction;

    @BeforeEach
    void setUp() {
        request = new TransferRequest();
        request.setSenderUserId(1L);
        request.setReceiverUserId(2L);
        request.setAmount(new BigDecimal("100"));
        request.setReferenceId("TXN-001");

        UserEntity senderUser = new UserEntity();
        senderUser.setId(1L);

        UserEntity receiverUser = new UserEntity();
        receiverUser.setId(2L);

        senderWallet = new WalletEntity();
        senderWallet.setId(1L);
        senderWallet.setUser(senderUser);
        senderWallet.setBalance(new BigDecimal("1000"));

        receiverWallet = new WalletEntity();
        receiverWallet.setId(2L);
        receiverWallet.setUser(receiverUser);
        receiverWallet.setBalance(new BigDecimal("500"));

        systemWallet = new WalletEntity();
        systemWallet.setId(99L);
        systemWallet.setBalance(BigDecimal.ZERO);

        transaction = new TransactionEntity();
        transaction.setId(10L);
        transaction.setReferenceId("TXN-001");
        transaction.setAmount(new BigDecimal("100"));
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setType(TransactionType.TRANSFER);
    }

    @Test
    void transferMoney_Success() {
        when(transactionRepository.findByReferenceId("TXN-001"))
                .thenReturn(Optional.empty());

        when(fraudService.checkTransferFraud(anyLong(), any()))
                .thenReturn(new FraudCheckResult(false, false, null));

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        when(systemWalletProvider.getSystemWalletId())
                .thenReturn(99L);

        when(walletRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.of(systemWallet));

        when(transactionMapper.toEntity(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(transaction);

        when(feeService.calculateFee(any()))
                .thenReturn(new BigDecimal("10"));

        LedgerEntryEntity debitEntry = new LedgerEntryEntity();
        debitEntry.setAmount(new BigDecimal("110"));

        LedgerEntryEntity creditEntry = new LedgerEntryEntity();
        creditEntry.setAmount(new BigDecimal("100"));

        LedgerEntryEntity feeEntry = new LedgerEntryEntity();
        feeEntry.setAmount(new BigDecimal("10"));

        when(ledgerEntryMapper.toEntity(
                any(),
                any(),
                any(),
                eq(EntryType.DEBIT)))
                .thenReturn(debitEntry);

        when(ledgerEntryMapper.toEntity(
                any(),
                eq(receiverWallet),
                any(),
                eq(EntryType.CREDIT)))
                .thenReturn(creditEntry);

        when(ledgerEntryMapper.toEntity(
                any(),
                eq(systemWallet),
                any(),
                eq(EntryType.CREDIT)))
                .thenReturn(feeEntry);

        TransferResponse expected =
                new TransferResponse(
                        10L,
                        "SUCCESS",
                        new BigDecimal("100"),
                        "TXN-001");

        when(transactionMapper.toTransferResponse(any()))
                .thenReturn(expected);

        TransferResponse response =
                transactionService.transferMoney(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());

        verify(rateLimiterService).checkRateLimit(1L);

        verify(transactionRepository, atLeastOnce())
                .save(any(TransactionEntity.class));

        verify(ledgerRepository)
                .saveAll(anyList());

        verify(walletRepository)
                .saveAll(anyList());

        verify(publisher).publishEvent(any(TransferCompletedEvent.class));
    }

    @Test
    void transferMoney_IdempotentRequest() {

        when(transactionRepository.findByReferenceId("TXN-001"))
                .thenReturn(Optional.of(transaction));

        TransferResponse expected =
                new TransferResponse(
                        10L,
                        "SUCCESS",
                        new BigDecimal("100"),
                        "TXN-001");

        when(transactionMapper.toTransferResponse(transaction))
                .thenReturn(expected);

        TransferResponse response =
                transactionService.transferMoney(request);

        assertEquals("SUCCESS", response.getStatus());

        verify(rateLimiterService, never())
                .checkRateLimit(anyLong());
    }

    @Test
    void transferMoney_InvalidAmount() {

        request.setAmount(BigDecimal.ZERO);
        // IDEMPOTENCY FIRST
        when(transactionRepository.findByReferenceId("TXN-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidTransactionException.class,
                () -> transactionService.transferMoney(request));
    }

    @Test
    void transferMoney_SameSenderReceiver() {

        request.setReceiverUserId(1L);

        // IDEMPOTENCY FIRST
        when(transactionRepository.findByReferenceId("TXN-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidTransactionException.class,
                () -> transactionService.transferMoney(request));
    }

    @Test
    void transferMoney_InsufficientBalance() {

        senderWallet.setBalance(new BigDecimal("50"));

        when(transactionRepository.findByReferenceId("TXN-001"))
                .thenReturn(Optional.empty());

        when(fraudService.checkTransferFraud(anyLong(), any()))
                .thenReturn(new FraudCheckResult(false, false, null));

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        when(systemWalletProvider.getSystemWalletId())
                .thenReturn(99L);

        when(walletRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.of(systemWallet));

        when(transactionMapper.toEntity(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(transaction);

        when(feeService.calculateFee(any()))
                .thenReturn(new BigDecimal("10"));

        assertThrows(
                InsufficientBalanceException.class,
                () -> transactionService.transferMoney(request));
    }

    @Test
    void transferMoney_FraudBlocked() {

        when(transactionRepository.findByReferenceId("TXN-001"))
                .thenReturn(Optional.empty());

        when(fraudService.checkTransferFraud(anyLong(), any()))
                .thenReturn(
                        new FraudCheckResult(
                                true,
                                true,
                                "Fraud detected"));

        assertThrows(
                InvalidTransactionException.class,
                () -> transactionService.transferMoney(request));
    }
}