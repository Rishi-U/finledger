package com.rishi.finledger.service.wallet;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rishi.finledger.dto.TransactionResponse;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.exception.UserNotFoundException;
import com.rishi.finledger.mapper.TransactionMapper;
import com.rishi.finledger.repository.TransactionRepository;
import com.rishi.finledger.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionHistoryService {

    private static final Logger log = LoggerFactory.getLogger(TransactionHistoryService.class);

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    public TransactionHistoryService (TransactionRepository transactionRepository, UserRepository userRepository, TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.transactionMapper = transactionMapper;
    }

    public List<TransactionResponse> getTransactionsByUserId(Long userId) {

        log.info("Fetching transaction history | userId={}", userId);
        
        if (!userRepository.existsById(userId)) {

            log.warn("User not found | userId={}", userId);

            throw new UserNotFoundException("User not found with id: " + userId);
        }

        List<TransactionEntity> transactions = transactionRepository.findBySender_User_IdOrReceiver_User_Id(userId, userId);


        log.info("Transaction history fetched | userId={} | totalTransactions={}", userId, transactions.size());

        return transactions.stream()
            .map(transactionMapper :: toTransactionResponse)
            .toList();
    }
}
