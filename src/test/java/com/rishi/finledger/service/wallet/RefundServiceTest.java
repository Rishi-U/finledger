package com.rishi.finledger.service.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;


import com.rishi.finledger.dto.FraudCheckResult;
import com.rishi.finledger.dto.RefundRequest;
import com.rishi.finledger.dto.TransferResponse;
import com.rishi.finledger.entity.EntryType;
import com.rishi.finledger.entity.LedgerEntryEntity;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;
import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.entity.WalletEntity;
import com.rishi.finledger.event.RefundCompletedEvent;
import com.rishi.finledger.exception.AuthorizationDeniedException;
import com.rishi.finledger.exception.InsufficientBalanceException;
import com.rishi.finledger.exception.InvalidTransactionException;
import com.rishi.finledger.mapper.LedgerEntryMapper;
import com.rishi.finledger.mapper.TransactionMapper;
import com.rishi.finledger.repository.LedgerRepository;
import com.rishi.finledger.repository.TransactionRepository;
import com.rishi.finledger.repository.WalletRepository;
import com.rishi.finledger.util.TransactionFinalStateResolver;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private TransactionFinalStateResolver transactionFinalStateResolver;

    @Mock
    private FraudService fraudService;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private RefundService refundService;

    private RefundRequest request;
    private TransactionEntity originalTx;
    private TransactionEntity refundTx;

    private WalletEntity senderWallet;
    private WalletEntity receiverWallet;

    @BeforeEach
    void setUp() {

        request = new RefundRequest();
        request.setAmount(new BigDecimal("100"));
        request.setReferenceId("REF-001");

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
        receiverWallet.setBalance(new BigDecimal("1000"));

        originalTx = new TransactionEntity();
        originalTx.setId(10L);
        originalTx.setAmount(new BigDecimal("500"));
        originalTx.setStatus(TransactionStatus.SUCCESS);
        originalTx.setType(TransactionType.TRANSFER);
        originalTx.setSender(senderWallet);
        originalTx.setReceiver(receiverWallet);

        refundTx = new TransactionEntity();
        refundTx.setId(20L);
        refundTx.setReferenceId("REF-001");
        refundTx.setAmount(new BigDecimal("100"));
        refundTx.setStatus(TransactionStatus.PENDING);
        refundTx.setType(TransactionType.REFUND);
    }

    @Test
    void refundTransaction_Success() {

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.empty());

        when(transactionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(originalTx));

        when(transactionRepository.findByOriginalTransactionIdAndType(
                10L,
                TransactionType.REFUND))
                .thenReturn(List.of());

        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        when(fraudService.checkRefundFraud(
                anyLong(),
                anyLong(),
                any(),
                any()))
                .thenReturn(new FraudCheckResult(false, false, null));

        when(transactionMapper.toRefundEntity(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyLong(),
                any()))
                .thenReturn(refundTx);

        LedgerEntryEntity debit = new LedgerEntryEntity();
        debit.setAmount(new BigDecimal("100"));

        LedgerEntryEntity credit = new LedgerEntryEntity();
        credit.setAmount(new BigDecimal("100"));

        when(ledgerEntryMapper.toEntity(
                any(),
                any(),
                any(),
                eq(EntryType.DEBIT)))
                .thenReturn(debit);

        when(ledgerEntryMapper.toEntity(
                any(),
                any(),
                any(),
                eq(EntryType.CREDIT)))
                .thenReturn(credit);

        TransferResponse expected = new TransferResponse(
                20L,
                "SUCCESS",
                new BigDecimal("100"),
                "REF-001");

        when(transactionMapper.toTransferResponse(any()))
                .thenReturn(expected);

        TransferResponse response = refundService.refundTransaction(10L, request, receiverWallet.getUser().getId());

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());

        verify(ledgerRepository)
                .saveAll(anyList());

        verify(walletRepository)
                .saveAll(anyList());

        verify(transactionRepository, atLeastOnce())
                .save(any(TransactionEntity.class));

        verify(publisher).publishEvent(any(RefundCompletedEvent.class));
    }

    @Test
    void refundTransaction_IdempotentRequest() {

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.of(refundTx));

        TransferResponse expected = new TransferResponse(
                20L,
                "SUCCESS",
                new BigDecimal("100"),
                "REF-001");

        when(transactionMapper.toTransferResponse(refundTx))
                .thenReturn(expected);

        TransferResponse response = refundService.refundTransaction(10L, request, receiverWallet.getUser().getId());

        assertEquals("SUCCESS", response.getStatus());

        verify(transactionRepository, never())
                .findByIdForUpdate(anyLong());
    }

    @Test
    void refundTransaction_TransactionNotFound() {

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.empty());

        when(transactionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidTransactionException.class,
                () -> refundService.refundTransaction(10L, request, receiverWallet.getUser().getId()));
    }

    @Test
    void refundTransaction_InvalidRefundAmount() {

        request.setAmount(BigDecimal.ZERO);

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.empty());

        when(transactionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(originalTx));

        assertThrows(
                InvalidTransactionException.class,
                () -> refundService.refundTransaction(10L, request, receiverWallet.getUser().getId()));
    }

    @Test
    void refundTransaction_FullyRefunded() {

        TransactionEntity oldRefund = new TransactionEntity();
        oldRefund.setAmount(new BigDecimal("500"));
        oldRefund.setStatus(TransactionStatus.SUCCESS);

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.empty());

        when(transactionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(originalTx));

        when(transactionRepository.findByOriginalTransactionIdAndType(
                10L,
                TransactionType.REFUND))
                .thenReturn(List.of(oldRefund));

        assertThrows(
                InvalidTransactionException.class,
                () -> refundService.refundTransaction(10L, request, receiverWallet.getUser().getId()));
    }

    @Test
    void refundTransaction_RefundExceedsRemaining() {

        request.setAmount(new BigDecimal("600"));

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.empty());

        when(transactionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(originalTx));

        when(transactionRepository.findByOriginalTransactionIdAndType(
                10L,
                TransactionType.REFUND))
                .thenReturn(List.of());

        assertThrows(
                InvalidTransactionException.class,
                () -> refundService.refundTransaction(10L, request, receiverWallet.getUser().getId()));
    }

    @Test
    void refundTransaction_InsufficientBalance() {

        receiverWallet.setBalance(new BigDecimal("50"));

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.empty());

        when(transactionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(originalTx));

        when(transactionRepository.findByOriginalTransactionIdAndType(
                10L,
                TransactionType.REFUND))
                .thenReturn(List.of());

        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        assertThrows(
                InsufficientBalanceException.class,
                () -> refundService.refundTransaction(10L, request, receiverWallet.getUser().getId()));
    }

    @Test
    void refundTransaction_FraudBlocked() {

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.empty());

        when(transactionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(originalTx));

        when(transactionRepository.findByOriginalTransactionIdAndType(
                10L,
                TransactionType.REFUND))
                .thenReturn(List.of());

        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        when(fraudService.checkRefundFraud(
                anyLong(),
                anyLong(),
                any(),
                any()))
                .thenReturn(
                        new FraudCheckResult(
                                true,
                                true,
                                "Fraud refund blocked"));

        assertThrows(
                InvalidTransactionException.class,
                () -> refundService.refundTransaction(10L, request, receiverWallet.getUser().getId()));
    }

    @Test
    void refundTransaction_LedgerImbalance() {

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.empty());

        when(transactionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(originalTx));

        when(transactionRepository.findByOriginalTransactionIdAndType(
                10L,
                TransactionType.REFUND))
                .thenReturn(List.of());

        when(walletRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        when(fraudService.checkRefundFraud(
                anyLong(),
                anyLong(),
                any(),
                any()))
                .thenReturn(new FraudCheckResult(false, false, null));

        when(transactionMapper.toRefundEntity(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyLong(),
                any()))
                .thenReturn(refundTx);

        LedgerEntryEntity debit = new LedgerEntryEntity();
        debit.setAmount(new BigDecimal("100"));

        LedgerEntryEntity credit = new LedgerEntryEntity();
        credit.setAmount(new BigDecimal("90"));

        when(ledgerEntryMapper.toEntity(
                any(),
                any(),
                any(),
                eq(EntryType.DEBIT)))
                .thenReturn(debit);

        when(ledgerEntryMapper.toEntity(
                any(),
                any(),
                any(),
                eq(EntryType.CREDIT)))
                .thenReturn(credit);

        assertThrows(
                InvalidTransactionException.class,
                () -> refundService.refundTransaction(10L, request, receiverWallet.getUser().getId()));
    }

    @Test
    void refundTransaction_UserNotOwner() {

        when(transactionRepository.findByReferenceId("REF-001"))
                .thenReturn(Optional.empty());

        when(transactionRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(originalTx));

        assertThrows(
                AuthorizationDeniedException.class,
                () -> refundService.refundTransaction(
                        10L,
                        request,
                        999L)); // attacker
    }
}