package com.rishi.finledger.service.wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

@Service
public class FeeService {
    private static final BigDecimal STND_FEE_PERCENT = new BigDecimal("0.01"); // 1%
    private static final BigDecimal MID_FEE_PERCENT = new BigDecimal("0.02"); // 2%
    private static final BigDecimal FEE_PERCENT = new BigDecimal("0.03"); // 3%

    // Thresholds
    private static final BigDecimal LOW_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal MID_THRESHOLD = new BigDecimal("50000");

    public BigDecimal calculateFee(BigDecimal amount) {
        if (amount.compareTo(LOW_THRESHOLD) <= 0) {
            return amount
                    .multiply(STND_FEE_PERCENT)
                    .setScale(2, RoundingMode.HALF_UP);
        } else if (amount.compareTo(MID_THRESHOLD) <= 0) {
            return amount
                    .multiply(MID_FEE_PERCENT)
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            return amount
                    .multiply(FEE_PERCENT)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
}
