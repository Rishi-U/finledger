package com.rishi.finledger.listener;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.rishi.finledger.event.TransferCompletedEvent;

@Component
public class FraudAnalyticsListener {

    private static final Logger log =
            LoggerFactory.getLogger(FraudAnalyticsListener.class);

    private static final BigDecimal ALERT_LIMIT =
            BigDecimal.valueOf(50000);

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void analyzeTransfer(
            TransferCompletedEvent event) {

        try {

            if (event.getAmount().compareTo(ALERT_LIMIT) >= 0) {

                log.warn(
                        "FRAUD ANALYTICS ALERT | high value transfer | refId={} | amount={}",
                        event.getReferenceId(),
                        event.getAmount()
                );
            }

        } catch (Exception e) {
            log.error("FRAUD ANALYTICS FAILED", e);
        }
    }
}