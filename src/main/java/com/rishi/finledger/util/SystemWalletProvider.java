package com.rishi.finledger.util;

import org.springframework.stereotype.Component;

import com.rishi.finledger.entity.WalletEntity;
import com.rishi.finledger.repository.WalletRepository;


@Component
public class SystemWalletProvider {

    private final WalletRepository walletRepository;
    private Long systemWalletId;

    public SystemWalletProvider(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Long getSystemWalletId() {
        if (systemWalletId == null) {
            WalletEntity wallet = walletRepository
                .findByUserEmail(SystemAccount.SYSTEM_EMAIL)
                .orElseThrow(() -> new RuntimeException("System wallet not found"));

            systemWalletId = wallet.getId();
        }
        return systemWalletId;
    }
}