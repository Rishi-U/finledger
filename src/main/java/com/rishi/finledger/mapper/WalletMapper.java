package com.rishi.finledger.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.rishi.finledger.dto.WalletResponse;
import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.entity.WalletEntity;

@Component
public class WalletMapper {
    public WalletResponse toWalletResponse(WalletEntity wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance());
    }

    public WalletEntity toEntity (UserEntity user) {
        WalletEntity wallet = new WalletEntity();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        return wallet;
    }
}
