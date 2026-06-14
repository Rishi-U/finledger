package com.rishi.finledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_original_tx", columnList = "originalTransactionId"),
        @Index(name = "idx_sender_wallet", columnList = "sender_wallet_id"),
        @Index(name = "idx_receiver_wallet", columnList = "receiver_wallet_id"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
        @Index(name = "idx_type_status_created", columnList = "type,status,createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sender
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_wallet_id", nullable = false)
    private WalletEntity sender;

    // Receiver
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_wallet_id", nullable = false)
    private WalletEntity receiver;

    // Amount
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    // Type (NEW 🔥)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    // Reference ID
    @Column(nullable = false, unique = true)
    private String referenceId;

    // Description (NEW 🔥)
    private String description;

    @Column
    private Long originalTransactionId;

    @Column(nullable = false)
    private boolean flagged = false;

    private String fraudReason;

    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Auto timestamps
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}