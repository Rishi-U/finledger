package com.rishi.finledger.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.repository.UserRepository;
import com.rishi.finledger.service.wallet.WalletService;
import com.rishi.finledger.util.SystemAccount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SystemUserInitializer {

    private static final Logger log = LoggerFactory.getLogger(SystemUserInitializer.class);

    private final BCryptPasswordEncoder passwordEncoder;

    public SystemUserInitializer(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    CommandLineRunner initSystemUser(UserRepository userRepository,
            WalletService walletService) {

        return args -> {
            userRepository.findByEmail(SystemAccount.SYSTEM_EMAIL)
            .ifPresentOrElse(
                user -> {
                    log.info("System user already exists | userId={}", user.getId());
                    try {
                        walletService.createWallet(user);
                    } catch (Exception e) {
                        log.info("System wallet already exists (ignored)");
                    }
                },() -> {
                    try {
                        // CREATE USER
                        UserEntity systemUser = new UserEntity();
                        systemUser.setName("SYSTEM");
                        systemUser.setEmail(SystemAccount.SYSTEM_EMAIL);
                        systemUser.setPassword(passwordEncoder.encode(SystemAccount.SYSTEM_PASSWORD));
                        
                        UserEntity saved = userRepository.save(systemUser);

                        // CREATE WALLET
                        walletService.createWallet(saved);
                        log.info("System user + wallet created | userId={}", saved.getId());
                    } catch (Exception e) {
                        log.warn("System user creation race detected, retrying fetch...");
                        
                        userRepository.findByEmail(SystemAccount.SYSTEM_EMAIL)
                            .ifPresent(user -> log.info("Recovered existing system user | userId={}",
                            user.getId()));
                        }
                    });
                };
            }
        }