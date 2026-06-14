package com.rishi.finledger.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.rishi.finledger.entity.EntryType;
import com.rishi.finledger.entity.LedgerEntryEntity;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.WalletEntity;

@Component
public class LedgerEntryMapper {
    public LedgerEntryEntity toEntity(
            TransactionEntity tx,
            WalletEntity wallet,
            BigDecimal amount,
            EntryType type) {
        LedgerEntryEntity ledger = new LedgerEntryEntity();
        ledger.setWallet(wallet);
        ledger.setTransaction(tx);
        ledger.setAmount(amount);
        ledger.setEntryType(type);
        return ledger;
    }
}
