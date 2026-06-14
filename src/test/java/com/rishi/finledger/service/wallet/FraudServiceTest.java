package com.rishi.finledger.service.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rishi.finledger.dto.FraudCheckResult;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;
import com.rishi.finledger.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class FraudServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private FraudService fraudService;

    private TransactionEntity transaction;

    @BeforeEach
    void setUp() {

        transaction = new TransactionEntity();
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("100"));
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setType(TransactionType.TRANSFER);
    }

    // =========================
    // TRANSFER FRAUD TESTS
    // =========================

    @Test
    void checkTransferFraud_HighValueTransaction() {

        FraudCheckResult result = fraudService.checkTransferFraud(1L, new BigDecimal("100001"));

        assertTrue(result.isBlocked());
        assertTrue(result.isFlagged());

        assertEquals(
            "High value transaction blocked",
            result.getReason());
    }

    @Test
    void checkTransferFraud_RapidTransactionsDetected() {

        when(transactionRepository.findRecentSuccessfulTransfers(
            eq(1L),
            any(LocalDateTime.class)))
            .thenReturn(List.of(
                transaction,
                transaction,
                transaction,
                transaction,
                transaction));

        FraudCheckResult result = fraudService.checkTransferFraud(
            1L,
            new BigDecimal("100"));

        assertFalse(result.isBlocked());
        assertTrue(result.isFlagged());

        assertEquals(
            "Rapid transaction activity detected",
            result.getReason());

        verify(transactionRepository)
            .findRecentSuccessfulTransfers(
                eq(1L),
                any(LocalDateTime.class));
    }

    @Test
    void checkTransferFraud_NoFraudDetected() {

        when(transactionRepository.findRecentSuccessfulTransfers(
            eq(1L),
            any(LocalDateTime.class)))
            .thenReturn(List.of(transaction));

        FraudCheckResult result = fraudService.checkTransferFraud(
            1L,
            new BigDecimal("500"));

        assertFalse(result.isBlocked());
        assertFalse(result.isFlagged());

        assertNull(result.getReason());
    }

    // =========================
    // REFUND FRAUD TESTS
    // =========================

    @Test
    void checkRefundFraud_InvalidOriginalAmount() {

        FraudCheckResult result = fraudService.checkRefundFraud(
            1L,
            2L,
            new BigDecimal("100"),
            BigDecimal.ZERO);

        assertTrue(result.isBlocked());
        assertTrue(result.isFlagged());

        assertEquals(
            "Invalid original transaction amount",
            result.getReason());
    }

    @Test
    void checkRefundFraud_LargeRefundPercentage() {

        FraudCheckResult result = fraudService.checkRefundFraud(
            1L,
            2L,
            new BigDecimal("900"),
            new BigDecimal("1000"));

        assertFalse(result.isBlocked());
        assertTrue(result.isFlagged());

        assertEquals(
            "Large refund percentage detected",
            result.getReason());
    }

    @Test
    void checkRefundFraud_MultipleRefundsDetected() {

        when(transactionRepository.findRefundTransactionsBySender(
            2L,
            TransactionType.REFUND,
            TransactionStatus.SUCCESS))
            .thenReturn(List.of(
                transaction,
                transaction,
                transaction,
                transaction,
                transaction));

        FraudCheckResult result = fraudService.checkRefundFraud(
            1L,
            2L,
            new BigDecimal("100"),
            new BigDecimal("1000"));

        assertFalse(result.isBlocked());
        assertTrue(result.isFlagged());

        assertEquals(
            "Multiple refunds detected",
            result.getReason());

        verify(transactionRepository)
            .findRefundTransactionsBySender(
                2L,
                TransactionType.REFUND,
                TransactionStatus.SUCCESS);
    }

    @Test
    void checkRefundFraud_NoFraudDetected() {

        when(transactionRepository.findRefundTransactionsBySender(
                2L,
                TransactionType.REFUND,
                TransactionStatus.SUCCESS))
                .thenReturn(List.of());

        FraudCheckResult result = fraudService.checkRefundFraud(
                1L,
                2L,
                new BigDecimal("100"),
                new BigDecimal("1000"));

        assertFalse(result.isBlocked());
        assertFalse(result.isFlagged());

        assertNull(result.getReason());
    }
}