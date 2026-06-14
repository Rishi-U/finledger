package com.rishi.finledger.event;

import java.math.BigDecimal;

public class RefundCompletedEvent {

    private final Long refundTransactionId;
    private final Long originalTransactionId;
    private final String referenceId;
    private final BigDecimal amount;

    public RefundCompletedEvent(
            Long refundTransactionId,
            Long originalTransactionId,
            String referenceId,
            BigDecimal amount) {

        this.refundTransactionId = refundTransactionId;
        this.originalTransactionId = originalTransactionId;
        this.referenceId = referenceId;
        this.amount = amount;
    }

    public Long getRefundTransactionId() {
        return refundTransactionId;
    }

    public Long getOriginalTransactionId() {
        return originalTransactionId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}