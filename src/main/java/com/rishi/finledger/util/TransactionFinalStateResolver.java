package com.rishi.finledger.util;

import org.springframework.stereotype.Component;

import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.repository.TransactionRepository;

@Component
public class TransactionFinalStateResolver {
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 100;
    private final TransactionRepository transactionRepository;

    public TransactionFinalStateResolver (TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionEntity waitForFinalState(String referenceId) {

        int retries = MAX_RETRIES;

        while (retries-- > 0) {
            TransactionEntity tx = transactionRepository
                    .findByReferenceId(referenceId)
                    .orElseThrow();

            if (tx.getStatus() != TransactionStatus.PENDING) {
                return tx;
            }

            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return transactionRepository
                .findByReferenceId(referenceId)
                .orElseThrow();
    }
}
