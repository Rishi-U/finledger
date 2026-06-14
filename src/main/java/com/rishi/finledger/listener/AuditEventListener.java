package com.rishi.finledger.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.rishi.finledger.event.TransferCompletedEvent;
import com.rishi.finledger.event.RefundCompletedEvent;

@Component
public class AuditEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(AuditEventListener.class);

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleTransactionSuccess(
            TransferCompletedEvent event) {

        try {
            log.info(
                    "AUDIT TRANSFER | txId={} | refId={} | sender={} | receiver={} | amount={}",
                    event.getTransactionId(),
                    event.getReferenceId(),
                    event.getSenderUserId(),
                    event.getReceiverUserId(),
                    event.getAmount()
            );

        } catch (Exception e) {
            log.error("AUDIT TRANSFER FAILED", e);
        }
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleRefundSuccess(
            RefundCompletedEvent event) {

        try {
            log.info(
                    "AUDIT REFUND | refundTxId={} | originalTxId={} | refId={} | amount={}",
                    event.getRefundTransactionId(),
                    event.getOriginalTransactionId(),
                    event.getReferenceId(),
                    event.getAmount()
            );

        } catch (Exception e) {
            log.error("AUDIT REFUND FAILED", e);
        }
    }
}