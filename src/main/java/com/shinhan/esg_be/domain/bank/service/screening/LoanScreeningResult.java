package com.shinhan.esg_be.domain.bank.service.screening;

import java.math.BigDecimal;

public record LoanScreeningResult(
        boolean approved,
        String reason,
        Long loanLimit,
        BigDecimal appliedRate,
        boolean specialApproval
) {

    public static LoanScreeningResult approve(String reason, long loanLimit, BigDecimal appliedRate) {
        return new LoanScreeningResult(true, reason, loanLimit, appliedRate, false);
    }

    public static LoanScreeningResult specialApprove(String reason, long loanLimit, BigDecimal appliedRate) {
        return new LoanScreeningResult(true, reason, loanLimit, appliedRate, true);
    }

    public static LoanScreeningResult reject(String reason) {
        return new LoanScreeningResult(false, reason, null, null, false);
    }
}
