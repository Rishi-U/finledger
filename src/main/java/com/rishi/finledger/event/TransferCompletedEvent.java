package com.rishi.finledger.event;

import java.math.BigDecimal;

public class TransferCompletedEvent {

    private final Long transactionId;
    private final String referenceId;
    private final Long senderUserId;
    private final Long receiverUserId;
    private final BigDecimal amount;

    public TransferCompletedEvent(
            Long transactionId,
            String referenceId,
            Long senderUserId,
            Long receiverUserId,
            BigDecimal amount) {

        this.transactionId = transactionId;
        this.referenceId = referenceId;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.amount = amount;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public Long getReceiverUserId() {
        return receiverUserId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}