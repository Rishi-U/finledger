package com.rishi.finledger.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishi.finledger.Security.JwtService;
import com.rishi.finledger.dto.RefundRequest;
import com.rishi.finledger.entity.Role;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.entity.WalletEntity;
import com.rishi.finledger.repository.LedgerRepository;
import com.rishi.finledger.repository.TransactionRepository;
import com.rishi.finledger.repository.UserRepository;
import com.rishi.finledger.repository.WalletRepository;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.TransactionType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RefundControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JwtService jwtService;

    private UserEntity sender;
    private UserEntity receiver;
    private WalletEntity senderWallet;
    private WalletEntity receiverWallet;
    private TransactionEntity originalTX;

    private String token;
    private String adminToken;

    private UserEntity attacker;
    @SuppressWarnings("unused")
    private WalletEntity attackerWallet;
    private String attackerToken;

    @BeforeEach
    void setup() {
        ledgerRepository.deleteAll();
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        sender = createUser("Dilip", "dilip@gmail.com", Role.USER);

        receiver = createUser("Rishi", "rishi@gmail.com", Role.USER);

        attacker = createUser("Hacker", "hacker@gmail.com", Role.USER);

        UserEntity admin = createUser("Admin", "admin@gmail.com", Role.ADMIN);
        
        senderWallet = createWallet(sender, BigDecimal.valueOf(1000));
        receiverWallet = createWallet(receiver, BigDecimal.valueOf(500));
        attackerWallet = createWallet(attacker, BigDecimal.valueOf(100));

        
        originalTX = new TransactionEntity();
        originalTX.setSender(senderWallet);
        originalTX.setReceiver(receiverWallet);
        originalTX.setAmount(BigDecimal.valueOf(100));
        originalTX.setReferenceId("TXN-001");
        originalTX.setStatus(TransactionStatus.SUCCESS);
        originalTX.setType(TransactionType.TRANSFER);

        originalTX = transactionRepository.save(originalTX);
        
        token = jwtService.generateAccessToken(receiver);
        adminToken = jwtService.generateAccessToken(admin);    
        attackerToken = jwtService.generateAccessToken(attacker);

    }

    private UserEntity createUser(String name, String email, Role role) {

        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("password");
        user.setRole(role);

        return userRepository.save(user);
    }

    private WalletEntity createWallet(UserEntity user, BigDecimal balance) {

        WalletEntity wallet = new WalletEntity();
        wallet.setUser(user);
        wallet.setBalance(balance);

        return walletRepository.save(wallet);
    }

    @Test
    void refund_ShouldSucceed() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setAmount(BigDecimal.valueOf(50));
        request.setReferenceId("RFN-001");

        Long transactionID = originalTX.getId();

        mockMvc.perform(post("/transactions/{id}/refund", transactionID)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.referenceId").value("RFN-001"))
                .andReturn();

        TransactionEntity refundTx = transactionRepository.findByReferenceId("RFN-001").orElseThrow();

        assertEquals(TransactionType.REFUND, refundTx.getType());
        assertEquals(TransactionStatus.SUCCESS, refundTx.getStatus());
        assertTrue(refundTx.getAmount().compareTo(BigDecimal.valueOf(50)) == 0);
        assertEquals(originalTX.getId(), refundTx.getOriginalTransactionId());

        WalletEntity updatedSender = walletRepository.findById(senderWallet.getId()).orElseThrow();

        WalletEntity updatedReceiver = walletRepository.findById(receiverWallet.getId()).orElseThrow();

        assertTrue(updatedSender.getBalance().compareTo(BigDecimal.valueOf(1050)) == 0);

        assertTrue(updatedReceiver.getBalance().compareTo(BigDecimal.valueOf(450)) == 0);
    }

    @Test
    void refund_ShouldReturn403_WhenNoToken() throws Exception {

        RefundRequest request = new RefundRequest();
        request.setAmount(BigDecimal.valueOf(50));
        request.setReferenceId("RFN-001");

        Long transactionID = originalTX.getId();

        mockMvc.perform(post("/transactions/{id}/refund", transactionID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void refund_ShouldReturn403_WhenInvalidToken() throws Exception {

        RefundRequest request = new RefundRequest();
        request.setAmount(BigDecimal.valueOf(50));
        request.setReferenceId("RFN-001");

        Long transactionID = originalTX.getId();

        mockMvc.perform(post("/transactions/{id}/refund", transactionID)
                .header("Authorization", "Bearer Invalid-Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void refund_ShouldReturnTransactionNotFound() throws Exception {

        RefundRequest request = new RefundRequest();
        request.setAmount(BigDecimal.valueOf(50));
        request.setReferenceId("RFN-001");

        Long transactionId = 99999L;

        mockMvc.perform(post("/transactions/{id}/refund", transactionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    void refund_ExpiredJWTToken() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setAmount(BigDecimal.valueOf(50));
        request.setReferenceId("RFN-001");

        Long transactionID = originalTX.getId();

        String expiredToken = jwtService.generateExpiredToken(receiver);

        mockMvc.perform(post("/transactions/{id}/refund", transactionID)
                .header("Authorization", "Bearer " + expiredToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void refund_ShouldADMINRoleTest() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setAmount(BigDecimal.valueOf(50));
        request.setReferenceId("RFN-001");

        Long transactionID = originalTX.getId();

        mockMvc.perform(post("/transactions/{id}/refund", transactionID)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void refund_ShouldFail_WhenUserNotPartOfTransaction() throws Exception {

        RefundRequest request = new RefundRequest();
        request.setAmount(BigDecimal.valueOf(50));
        request.setReferenceId("RFN-HACK");

        mockMvc.perform(
                post("/transactions/{id}/refund", originalTX.getId())
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void refund_ShouldReject_WhenRefundExceedsRemainingAmount() throws Exception {

        Long transactionId = originalTX.getId();

        RefundRequest firstRefund = new RefundRequest();
        firstRefund.setAmount(BigDecimal.valueOf(50));
        firstRefund.setReferenceId("RFN-001");

        mockMvc.perform(post("/transactions/{id}/refund", transactionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRefund)))
                .andExpect(status().isOk());

        RefundRequest secondRefund = new RefundRequest();
        secondRefund.setAmount(BigDecimal.valueOf(60));
        secondRefund.setReferenceId("RFN-002");

        mockMvc.perform(post("/transactions/{id}/refund", transactionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRefund)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refund exceeds remaining amount"));
    }

    @Test
    void refund_ShouldIdempotencyAttackTest() throws Exception {

        Long transactionId = originalTX.getId();

        RefundRequest firstRefund = new RefundRequest();
        firstRefund.setAmount(BigDecimal.valueOf(50));
        firstRefund.setReferenceId("RFN-001");

        MvcResult firstResult = mockMvc.perform(post("/transactions/{id}/refund", transactionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRefund)))
                .andExpect(status().isOk())
                .andReturn();

        RefundRequest secondRefund = new RefundRequest();
        secondRefund.setAmount(BigDecimal.valueOf(50));
        secondRefund.setReferenceId("RFN-001");

        MvcResult secondResult = mockMvc.perform(post("/transactions/{id}/refund", transactionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRefund)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String firstResponse = firstResult.getResponse().getContentAsString();

        String secondResponse = secondResult.getResponse().getContentAsString();

        Long firstTxId = objectMapper
                .readTree(firstResponse)
                .get("transactionId")
                .asLong();

        Long secondTxId = objectMapper
                .readTree(secondResponse)
                .get("transactionId")
                .asLong();

        assertEquals(firstTxId, secondTxId);
    }
}
