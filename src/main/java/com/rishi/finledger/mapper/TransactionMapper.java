package com.rishi.finledger.mapper;

import org.springframework.stereotype.Component;

import com.rishi.finledger.dto.FraudCheckResult;
import com.rishi.finledger.dto.RefundRequest;
import com.rishi.finledger.dto.TransactionResponse;
import com.rishi.finledger.dto.TransferRequest;
import com.rishi.finledger.dto.TransferResponse;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;
import com.rishi.finledger.entity.WalletEntity;

@Component
public class TransactionMapper {
    public TransferResponse toTransferResponse(TransactionEntity tx) {
        return new TransferResponse(
                tx.getId(),
                tx.getStatus().name(),
                tx.getAmount(),
                tx.getReferenceId());
    }

    public TransactionResponse toTransactionResponse(TransactionEntity transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSender().getUser().getId(),
                transaction.getReceiver().getUser().getId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getType(),
                transaction.getCreatedAt());
    }

    public TransactionEntity toEntity(
            TransferRequest request,
            WalletEntity sender,
            WalletEntity receiver, 
            TransactionStatus status, 
            TransactionType type,
            FraudCheckResult fraud) {

        TransactionEntity tx = new TransactionEntity();

        tx.setSender(sender);
        tx.setReceiver(receiver);
        tx.setAmount(request.getAmount());
        tx.setStatus(status);
        tx.setType(type);
        tx.setReferenceId(request.getReferenceId());
        tx.setFlagged(fraud.isFlagged());
        tx.setFraudReason(fraud.getReason());
        return tx;
    }

    public TransactionEntity toRefundEntity(
            RefundRequest request,
            WalletEntity refundTo,
            WalletEntity refundFrom,
            TransactionStatus status,
            TransactionType type,
            Long originalTxId, 
            FraudCheckResult fraud) {
        TransactionEntity tx = new TransactionEntity();

        tx.setSender(refundFrom); // money leaving
        tx.setReceiver(refundTo); // money receiving

        tx.setAmount(request.getAmount());
        tx.setOriginalTransactionId(originalTxId);
        tx.setStatus(status);
        tx.setType(type);
        tx.setReferenceId(request.getReferenceId());

        tx.setDescription("REFUND of TX " + originalTxId);

        tx.setFlagged(fraud.isFlagged());
        tx.setFraudReason(fraud.getReason());

        return tx;
    }
}
