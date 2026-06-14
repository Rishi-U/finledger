package com.rishi.finledger.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishi.finledger.Security.JwtService;
import com.rishi.finledger.dto.FraudCheckResult;
import com.rishi.finledger.dto.TransferRequest;
import com.rishi.finledger.entity.EntryType;
import com.rishi.finledger.entity.LedgerEntryEntity;
import com.rishi.finledger.entity.Role;
import com.rishi.finledger.entity.TransactionEntity;
import com.rishi.finledger.entity.TransactionStatus;
import com.rishi.finledger.entity.UserEntity;
import com.rishi.finledger.entity.WalletEntity;
import com.rishi.finledger.repository.LedgerRepository;
import com.rishi.finledger.repository.TransactionRepository;
import com.rishi.finledger.repository.UserRepository;
import com.rishi.finledger.repository.WalletRepository;
import com.rishi.finledger.service.wallet.FraudService;
import com.rishi.finledger.service.wallet.TransactionService;
import com.rishi.finledger.util.SystemAccount;

@SuppressWarnings("unused")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private FraudService fraudService;

    @Autowired
    private TransactionService transactionService;

    private UserEntity sender;
    private UserEntity receiver;
    private UserEntity systemUser;
    private UserEntity attacker;

    private WalletEntity senderWallet;
    private WalletEntity receiverWallet;
    private WalletEntity systemWallet;

    private String token;

    @BeforeEach
    void setup() {
        ledgerRepository.deleteAll();
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        sender = createUser("Dilip", "dilip@gmail.com", Role.USER);
        receiver = createUser("Rishi", "rishi@gmail.com", Role.USER);
        systemUser = createUser("SYSTEM", SystemAccount.SYSTEM_EMAIL, Role.ADMIN);
        attacker = createUser("Attacker", "attacker@gmail.com", Role.USER);

        senderWallet = createWallet(sender, BigDecimal.valueOf(1000));
        receiverWallet = createWallet(receiver, BigDecimal.valueOf(500));
        systemWallet = createWallet(systemUser, BigDecimal.ZERO);
        WalletEntity attackerWallet = createWallet(attacker, BigDecimal.valueOf(1000));

        token = jwtService.generateAccessToken(sender);

        when(fraudService.checkTransferFraud(anyLong(), any(BigDecimal.class)))
                .thenReturn(new FraudCheckResult(false, false, null));
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
    void transfer_ShouldSucceed() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setSenderUserId(sender.getId());
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setReferenceId("TXN-001");

        MvcResult result = mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.referenceId").value("TXN-001"))
                .andReturn();

        System.out.println(result.getResponse().getStatus());
        System.out.println(result.getResponse().getContentAsString());

        WalletEntity updatedSender = walletRepository.findByUserId(sender.getId()).orElseThrow();

        WalletEntity updatedReceiver = walletRepository.findByUserId(receiver.getId()).orElseThrow();

        System.out.println("Sender = " + updatedSender.getBalance());
        System.out.println("Receiver = " + updatedReceiver.getBalance());

        assertEquals(new BigDecimal("899.00"), updatedSender.getBalance());
        assertEquals(new BigDecimal("600.00"), updatedReceiver.getBalance());
    }

    @Test
    void transfer_ShouldReturn403_WhenNoToken() throws Exception {

        TransferRequest request = new TransferRequest();
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setReferenceId("TXN-NO-TOKEN");

        mockMvc.perform(post("/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_ShouldReturn403_WhenInvalidToken() throws Exception {

        TransferRequest request = new TransferRequest();
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setReferenceId("TXN-BAD-TOKEN");

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_ShouldFail_WhenTransferToSelf() throws Exception {

        TransferRequest request = new TransferRequest();
        request.setReceiverUserId(sender.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setReferenceId("TXN-SELF");

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_ShouldFail_WhenAmountNegative() throws Exception {

        TransferRequest request = new TransferRequest();
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(-100));
        request.setReferenceId("TXN-NEG");

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_ShouldFail_WhenAmountZero() throws Exception {

        TransferRequest request = new TransferRequest();
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.ZERO);
        request.setReferenceId("TXN-ZERO");

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_ShouldFail_WhenInsufficientBalance() throws Exception {

        TransferRequest request = new TransferRequest();
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(100000));
        request.setReferenceId("TXN-INSUFFICIENT");

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_ShouldBeIdempotent() throws Exception {

        TransferRequest request = new TransferRequest();
        request.setSenderUserId(sender.getId());
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setReferenceId("TXN-IDEMPOTENT");

        MvcResult firstResult = mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        Long firstTxId = objectMapper.readTree(
                firstResult.getResponse().getContentAsString())
                .get("transactionId")
                .asLong();

        Long secondTxId = objectMapper.readTree(
                secondResult.getResponse().getContentAsString())
                .get("transactionId")
                .asLong();

        assertEquals(firstTxId, secondTxId);
    }

    @Test
    void transfer_ShouldRejectSenderSpoofing() throws Exception {

        TransferRequest request = new TransferRequest();
        request.setSenderUserId(attacker.getId());
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setReferenceId("TXN-SPOOF");

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.referenceId").value("TXN-SPOOF"));

        WalletEntity updatedSender = walletRepository.findByUserId(sender.getId()).orElseThrow();

        WalletEntity updatedReceiver = walletRepository.findByUserId(receiver.getId()).orElseThrow();

        WalletEntity updatedAttacker = walletRepository.findByUserId(attacker.getId()).orElseThrow();

        System.out.println("Sender   = " + updatedSender.getBalance());
        System.out.println("Receiver = " + updatedReceiver.getBalance());
        System.out.println("Attacker = " + updatedAttacker.getBalance());

        assertEquals(new BigDecimal("899.00"), updatedSender.getBalance());

        // Receiver received money
        assertEquals(new BigDecimal("600.00"), updatedReceiver.getBalance());

        // Spoofed account untouched
        assertEquals(new BigDecimal("1000.00"), updatedAttacker.getBalance());
    }

    @Test
    void transfer_ShouldPreserveWalletConservation() throws Exception {
        BigDecimal before = senderWallet.getBalance().add(receiverWallet.getBalance()).add(systemWallet.getBalance());

        TransferRequest request = new TransferRequest();
        request.setSenderUserId(sender.getId());
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setReferenceId("TXN-SPOOF");

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.referenceId").value("TXN-SPOOF"));

        WalletEntity updatedSender = walletRepository.findByUserId(sender.getId()).orElseThrow();

        WalletEntity updatedReceiver = walletRepository.findByUserId(receiver.getId()).orElseThrow();

        WalletEntity updatedSystem = walletRepository.findByUserId(systemUser.getId()).orElseThrow();

        BigDecimal after = updatedSender.getBalance().add(updatedReceiver.getBalance()).add(updatedSystem.getBalance());

        assertEquals(0, before.compareTo(after));

    }

    @Test
    void transfer_ShouldMaintainLedgerBalance() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setSenderUserId(sender.getId());
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setReferenceId("TXN-LEDGER");

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.referenceId").value("TXN-LEDGER"));

        TransactionEntity tx = transactionRepository.findByReferenceId("TXN-LEDGER").orElseThrow();

        List<LedgerEntryEntity> entries = ledgerRepository.findByTransactionId(tx.getId());

        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;

        for (LedgerEntryEntity entry : entries) {

            if (entry.getEntryType() == EntryType.DEBIT) {
                debit = debit.add(entry.getAmount());
            }

            if (entry.getEntryType() == EntryType.CREDIT) {
                credit = credit.add(entry.getAmount());
            }
        }

        assertEquals(3, entries.size());
        assertEquals(0, debit.compareTo(credit));
    }

    @Test
    void transfer_ShouldFraudCheck() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setSenderUserId(sender.getId());
        request.setReceiverUserId(receiver.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setReferenceId("TXN-FRAUD");

        when(fraudService.checkTransferFraud(request.getSenderUserId(), request.getAmount()))
                .thenReturn(new FraudCheckResult(true, false, "BLOCKED"));

        mockMvc.perform(post("/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        WalletEntity updatedSender = walletRepository.findByUserId(sender.getId()).orElseThrow();

        WalletEntity updatedReceiver = walletRepository.findByUserId(receiver.getId()).orElseThrow();

        assertEquals(new BigDecimal("1000.00"), updatedSender.getBalance());

        assertEquals(new BigDecimal("500.00"), updatedReceiver.getBalance());
    }

    @Test
    void transfer_ShouldPreventDoubleClickAttack() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Void> task = () -> {
            TransferRequest request = new TransferRequest();
            request.setSenderUserId(sender.getId());
            request.setReceiverUserId(receiver.getId());
            request.setAmount(BigDecimal.valueOf(100));
            request.setReferenceId("TXN-RACE");

            try {
                transactionService.transferMoney(request);
            } catch (Exception ignored) {
            }

            return null;
        };

        Future<Void> f1 = executor.submit(task);
        Future<Void> f2 = executor.submit(task);

        f1.get();
        f2.get();

        executor.shutdown();

        long count = transactionRepository.findAll()
                .stream()
                .filter(tx -> "TXN-RACE".equals(tx.getReferenceId()))
                .count();

        assertEquals(1, count);
    }

    @Test
    void transfer_ShouldPreventDoubleSpendAttack() throws Exception {
        Callable<Void> task1 = () -> {

            TransferRequest request = new TransferRequest();

            request.setSenderUserId(sender.getId());
            request.setReceiverUserId(receiver.getId());

            request.setAmount(BigDecimal.valueOf(900));
            request.setReferenceId("TXN-1");

            try {
                transactionService.transferMoney(request);
            } catch (Exception ignored) {
            }

            return null;
        };
        Callable<Void> task2 = () -> {

            TransferRequest request = new TransferRequest();

            request.setSenderUserId(sender.getId());
            request.setReceiverUserId(receiver.getId());

            request.setAmount(BigDecimal.valueOf(900));
            request.setReferenceId("TXN-2");

            try {
                transactionService.transferMoney(request);
            } catch (Exception ignored) {
            }

            return null;
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        f1.get();
        f2.get();

        executor.shutdown();

        long successCount = transactionRepository.findAll().stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.SUCCESS).count();

        assertEquals(1, successCount);

        WalletEntity updated = walletRepository.findByUserId(sender.getId()).orElseThrow();

        assertTrue(updated.getBalance().compareTo(BigDecimal.ZERO) >= 0);
        assertEquals(new BigDecimal("91.00"), updated.getBalance());
    }
}
