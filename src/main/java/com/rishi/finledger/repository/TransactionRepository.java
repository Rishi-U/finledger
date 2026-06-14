package com.rishi.finledger.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;

import jakarta.persistence.LockModeType;

public interface TransactionRepository
        extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findBySender_User_IdOrReceiver_User_Id(
            Long senderId,
            Long receiverId);

    Optional<TransactionEntity> findByReferenceId(String referenceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t
        FROM TransactionEntity t
        WHERE t.id = :id
    """)
    Optional<TransactionEntity> findByIdForUpdate(Long id);

    List<TransactionEntity> findByOriginalTransactionIdAndType(
            Long originalTransactionId,
            TransactionType type);

    // RAPID TRANSFER DETECTION
    @Query("""
        SELECT t
        FROM TransactionEntity t
        WHERE t.sender.user.id = :userId
        AND t.type = 'TRANSFER'
        AND t.status = 'SUCCESS'
        AND t.createdAt >= :time
    """)
    List<TransactionEntity> findRecentSuccessfulTransfers(
            Long userId,
            LocalDateTime time);

    // REFUND MONITORING
    @Query("""
        SELECT t
        FROM TransactionEntity t
        WHERE t.sender.user.id = :userId
        AND t.type = :type
        AND t.status = :status
    """)
    List<TransactionEntity> findRefundTransactionsBySender(
            Long userId,
            TransactionType type,
            TransactionStatus status);
}