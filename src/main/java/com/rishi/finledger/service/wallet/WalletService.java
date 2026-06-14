package com.rishi.finledger.service.wallet;

import org.springframework.stereotype.Service;

import com.rishi.finledger.dto.WalletResponse;
import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.entity.WalletEntity;
import com.rishi.finledger.exception.UserNotFoundException;
import com.rishi.finledger.exception.WalletAlreadyExistsException;
import com.rishi.finledger.exception.WalletNotFoundException;
import com.rishi.finledger.mapper.WalletMapper;
import com.rishi.finledger.repository.UserRepository;
import com.rishi.finledger.repository.WalletRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final UserRepository usersRepository;
    private final WalletMapper walletMapper;

    public WalletService(WalletRepository walletRepository, UserRepository usersRepository, WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.usersRepository = usersRepository;
        this.walletMapper = walletMapper;
    }

    public WalletEntity createWallet(UserEntity user) {
        log.info("Wallet creation started | userId={}", user.getId());

        if (walletRepository.findByUserId(user.getId()).isPresent()) {
            log.warn("Wallet already exists | userId={}", user.getId());
            throw new WalletAlreadyExistsException("Wallet already exists for userId: " + user.getId());
        }

        WalletEntity wallet = walletMapper.toEntity(user);

        WalletEntity savedWallet = walletRepository.save(wallet);

        log.info("Wallet created successfully | walletId={} | userId={}",
                savedWallet.getId(),
                user.getId());

        return savedWallet;
    }

    public WalletResponse getWalletByUserId(Long userId) {
        log.info("Fetching wallet | userId={}", userId);

        // 🔥 Step 1: Check user exists
        if (!usersRepository.existsById(userId)) {
            log.warn("User not found | userId={}", userId);
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        // 🔥 Step 2: Get wallet
        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Wallet not found | userId={}", userId);
                    return new WalletNotFoundException("Wallet not found for userId: " + userId);
                });

        log.info("Wallet fetched successfully | walletId={} | userId={}",
                wallet.getId(),
                userId);

        return walletMapper.toWalletResponse(wallet);
    }
}
