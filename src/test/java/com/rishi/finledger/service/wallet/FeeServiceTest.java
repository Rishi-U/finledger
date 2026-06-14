package com.rishi.finledger.service.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeeServiceTest {

    private FeeService feeService;

    @BeforeEach
    void setUp() {
        feeService = new FeeService();
    }

    @Test
    void calculateFee_LowAmount() {

        BigDecimal fee =
                feeService.calculateFee(new BigDecimal("1000"));

        assertEquals(
                new BigDecimal("10.00"),
                fee);
    }

    @Test
    void calculateFee_LowThresholdBoundary() {

        BigDecimal fee =
                feeService.calculateFee(new BigDecimal("10000"));

        assertEquals(
                new BigDecimal("100.00"),
                fee);
    }

    @Test
    void calculateFee_MidAmount() {

        BigDecimal fee =
                feeService.calculateFee(new BigDecimal("20000"));

        assertEquals(
                new BigDecimal("400.00"),
                fee);
    }

    @Test
    void calculateFee_MidThresholdBoundary() {

        BigDecimal fee =
                feeService.calculateFee(new BigDecimal("50000"));

        assertEquals(
                new BigDecimal("1000.00"),
                fee);
    }

    @Test
    void calculateFee_HighAmount() {

        BigDecimal fee =
                feeService.calculateFee(new BigDecimal("100000"));

        assertEquals(
                new BigDecimal("3000.00"),
                fee);
    }

    @Test
    void calculateFee_RoundingCheck() {

        BigDecimal fee =
                feeService.calculateFee(new BigDecimal("999.99"));

        assertEquals(
                new BigDecimal("10.00"),
                fee);
    }
}