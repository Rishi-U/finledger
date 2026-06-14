package com.rishi.finledger.service.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rishi.finledger.dto.TransactionResponse;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;
import com.rishi.finledger.exception.UserNotFoundException;
import com.rishi.finledger.mapper.TransactionMapper;
import com.rishi.finledger.repository.TransactionRepository;
import com.rishi.finledger.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TransactionHistoryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionHistoryService transactionHistoryService;

    private TransactionEntity transaction;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {

        transaction = new TransactionEntity();
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("100"));
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setType(TransactionType.TRANSFER);

        transactionResponse = new TransactionResponse(
            1L,
            1L,
            2L,
            new BigDecimal("100"),
            TransactionStatus.SUCCESS,
            TransactionType.TRANSFER,
            LocalDateTime.now()
        );
    }

    @Test
    void getTransactionsByUserId_Success() {

        Long userId = 1L;

        when(userRepository.existsById(userId))
            .thenReturn(true);

        when(transactionRepository
            .findBySender_User_IdOrReceiver_User_Id(userId, userId))
            .thenReturn(List.of(transaction));

        when(transactionMapper.toTransactionResponse(transaction))
            .thenReturn(transactionResponse);

        List<TransactionResponse> result =
                transactionHistoryService.getTransactionsByUserId(userId);

        assertNotNull(result);
        assertEquals(1, result.size());

        assertEquals(1L, result.get(0).getTransactionId());
        assertEquals(TransactionStatus.SUCCESS, result.get(0).getStatus());

        verify(userRepository).existsById(userId);

        verify(transactionRepository)
                .findBySender_User_IdOrReceiver_User_Id(userId, userId);

        verify(transactionMapper)
                .toTransactionResponse(transaction);
    }

    @Test
    void getTransactionsByUserId_UserNotFound() {

        Long userId = 99L;

        when(userRepository.existsById(userId))
            .thenReturn(false);

        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> transactionHistoryService.getTransactionsByUserId(userId)
        );

        assertEquals(
            "User not found with id: 99",
            exception.getMessage()
        );

        verify(userRepository).existsById(userId);

        verify(transactionRepository, never())
            .findBySender_User_IdOrReceiver_User_Id(anyLong(), anyLong());
    }

    @Test
    void getTransactionsByUserId_EmptyTransactions() {

        Long userId = 1L;

        when(userRepository.existsById(userId))
            .thenReturn(true);

        when(transactionRepository
            .findBySender_User_IdOrReceiver_User_Id(userId, userId))
            .thenReturn(List.of());

        List<TransactionResponse> result = transactionHistoryService.getTransactionsByUserId(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(transactionRepository).findBySender_User_IdOrReceiver_User_Id(userId, userId);
    }
}