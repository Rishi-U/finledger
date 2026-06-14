package com.rishi.finledger.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rishi.finledger.entity.LedgerEntryEntity;

public interface LedgerRepository extends JpaRepository<LedgerEntryEntity, Long> {
    List<LedgerEntryEntity> findByTransactionId (Long transactionId);
}
