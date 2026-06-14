package com.rishi.finledger.service.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.rishi.finledger.dto.WalletResponse;
import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.entity.WalletEntity;
import com.rishi.finledger.exception.UserNotFoundException;
import com.rishi.finledger.exception.WalletAlreadyExistsException;
import com.rishi.finledger.exception.WalletNotFoundException;
import com.rishi.finledger.mapper.WalletMapper;
import com.rishi.finledger.repository.UserRepository;
import com.rishi.finledger.repository.WalletRepository;

class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateWalletSuccessfully() {

        // ARRANGE
        UserEntity user = new UserEntity();
        user.setId(1L);

        WalletEntity wallet = new WalletEntity();
        wallet.setId(10L);
        wallet.setBalance(BigDecimal.ZERO);

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        when(walletMapper.toEntity(user))
                .thenReturn(wallet);

        when(walletRepository.save(wallet))
                .thenReturn(wallet);

        // ACT
        WalletEntity result = walletService.createWallet(user);

        // ASSERT
        assertNotNull(result);
        assertEquals(10L, result.getId());

        verify(walletRepository).save(wallet);
    }

    @Test
    void shouldThrowExceptionWhenWalletAlreadyExists() {

        // ARRANGE
        UserEntity user = new UserEntity();
        user.setId(1L);

        WalletEntity existingWallet = new WalletEntity();

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.of(existingWallet));

        // ACT + ASSERT
        assertThrows(
                WalletAlreadyExistsException.class,
                () -> walletService.createWallet(user));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldReturnWalletSuccessfully() {

        // ARRANGE
        Long userId = 1L;

        WalletEntity wallet = new WalletEntity();
        wallet.setId(100L);
        wallet.setBalance(BigDecimal.valueOf(5000));

        WalletResponse response =
                new WalletResponse(100L, BigDecimal.valueOf(5000));

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(walletRepository.findByUserId(userId))
                .thenReturn(Optional.of(wallet));

        when(walletMapper.toWalletResponse(wallet))
                .thenReturn(response);

        // ACT
        WalletResponse result =
                walletService.getWalletByUserId(userId);

        // ASSERT
        assertNotNull(result);

        assertEquals(100L, result.getWalletId());

        assertEquals(
                BigDecimal.valueOf(5000),
                result.getBalance());

        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {

        // ARRANGE
        Long userId = 1L;

        when(userRepository.existsById(userId))
                .thenReturn(false);

        // ACT + ASSERT
        assertThrows(
                UserNotFoundException.class,
                () -> walletService.getWalletByUserId(userId));

        verify(walletRepository, never()).findByUserId(any());
    }

    @Test
    void shouldThrowExceptionWhenWalletNotFound() {

        // ARRANGE
        Long userId = 1L;

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(walletRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                WalletNotFoundException.class,
                () -> walletService.getWalletByUserId(userId));

        verify(walletRepository).findByUserId(userId);
    }
}