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
public class NotificationEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationEventListener.class);

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void sendTransferNotification(
            TransferCompletedEvent event) {

        try {
            log.info(
                    "NOTIFICATION SENT | TRANSFER | refId={} | amount={}",
                    event.getReferenceId(),
                    event.getAmount()
            );

        } catch (Exception e) {
            log.error("TRANSFER NOTIFICATION FAILED", e);
        }
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void sendRefundNotification(
            RefundCompletedEvent event) {

        try {
            log.info(
                    "NOTIFICATION SENT | REFUND | refId={} | amount={}",
                    event.getReferenceId(),
                    event.getAmount()
            );

        } catch (Exception e) {
            log.error("REFUND NOTIFICATION FAILED", e);
        }
    }
}