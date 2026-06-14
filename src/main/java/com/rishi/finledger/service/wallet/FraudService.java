package com.rishi.finledger.service.wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rishi.finledger.dto.FraudCheckResult;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;
import com.rishi.finledger.repository.TransactionRepository;

@Service
public class FraudService {

    private static final BigDecimal LARGE_TX_LIMIT = BigDecimal.valueOf(100000);

    private final TransactionRepository transactionRepository;

    public FraudService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // TRANSFER FRAUD CHECKS
    public FraudCheckResult checkTransferFraud(
            Long senderUserId,
            BigDecimal amount) {

        if (amount.compareTo(LARGE_TX_LIMIT) > 0) {
            return new FraudCheckResult(
                true,
                true,
                "High value transaction blocked");
        }

        // RAPID TRANSFER CHECK
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);

        List<TransactionEntity> recentTxs = transactionRepository.findRecentSuccessfulTransfers(
            senderUserId,
            oneMinuteAgo);

        if (recentTxs.size() >= 5) {
            return new FraudCheckResult(
                false,
                true,
                "Rapid transaction activity detected");
        }

        return new FraudCheckResult(false, false, null);
    }

    // REFUND FRAUD CHECKS
    public FraudCheckResult checkRefundFraud(
            Long originalTransactionId,
            Long receiverUserId,
            BigDecimal refundAmount,
            BigDecimal originalAmount) {

        if (originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new FraudCheckResult(
                true,
                true,
                "Invalid original transaction amount");
        }
        // LARGE REFUND RATIO
        BigDecimal refundPercent = refundAmount
            .divide(originalAmount, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        if (refundPercent.compareTo(BigDecimal.valueOf(80)) > 0) {
            return new FraudCheckResult(
                false,
                true,
                "Large refund percentage detected");
        }

        // TOO MANY REFUNDS
        List<TransactionEntity> refunds = transactionRepository.findRefundTransactionsBySender(
            receiverUserId,
            TransactionType.REFUND,
            TransactionStatus.SUCCESS);

        if (refunds.size() >= 5) {
            return new FraudCheckResult(
                false,
                true,
                "Multiple refunds detected");
        }

        return new FraudCheckResult(false, false, null);
    }
}
