package com.shinhan.esg_be.domain.bank.service.screening;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class LoanScreeningPolicy {

    public static final String REASON_AVAILABLE = "AVAILABLE";
    public static final String REASON_SPECIAL_APPROVED_LOW_CB = "SPECIAL_APPROVED_LOW_CB";
    public static final String REASON_LOAN_BLOCKED = "LOAN_BLOCKED";
    public static final String REASON_HAS_ACTIVE_LOAN = "HAS_ACTIVE_LOAN";
    public static final String REASON_TELECOM_DELINQUENCY = "TELECOM_DELINQUENCY";
    public static final String REASON_LOW_ESG_SCORE = "LOW_SCORE";
    public static final String REASON_CB_SCORE_TOO_LOW = "CB_SCORE_TOO_LOW";
    public static final String REASON_CB_SCORE_NOT_ELIGIBLE = "CB_SCORE_NOT_ELIGIBLE";

    private static final long LOAN_LIMIT_700 = 1_000_000L;
    private static final long LOAN_LIMIT_800 = 2_000_000L;
    private static final long LOAN_LIMIT_900 = 3_000_000L;
    private static final BigDecimal LOAN_RATE_700 = new BigDecimal("8.50");
    private static final BigDecimal LOAN_RATE_800 = new BigDecimal("7.00");
    private static final BigDecimal LOAN_RATE_900 = new BigDecimal("6.00");

    public LoanScreeningResult evaluate(
            int esgScore,
            int cbScore,
            boolean hasTelecomDelinquency,
            boolean hasActiveLoan,
            boolean loanBlocked
    ) {
        if (loanBlocked) {
            return LoanScreeningResult.reject(REASON_LOAN_BLOCKED);
        }

        if (hasActiveLoan) {
            return LoanScreeningResult.reject(REASON_HAS_ACTIVE_LOAN);
        }

        if (hasTelecomDelinquency) {
            return LoanScreeningResult.reject(REASON_TELECOM_DELINQUENCY);
        }

        if (cbScore >= 600) {
            return evaluateStandardOffer(esgScore);
        }

        if (cbScore >= 500) {
            if (esgScore >= 900) {
                return LoanScreeningResult.specialApprove(
                        REASON_SPECIAL_APPROVED_LOW_CB,
                        LOAN_LIMIT_700,
                        LOAN_RATE_700
                );
            }

            return LoanScreeningResult.reject(REASON_CB_SCORE_NOT_ELIGIBLE);
        }

        return LoanScreeningResult.reject(REASON_CB_SCORE_TOO_LOW);
    }

    private LoanScreeningResult evaluateStandardOffer(int esgScore) {
        if (esgScore >= 900) {
            return LoanScreeningResult.approve(REASON_AVAILABLE, LOAN_LIMIT_900, LOAN_RATE_900);
        }

        if (esgScore >= 800) {
            return LoanScreeningResult.approve(REASON_AVAILABLE, LOAN_LIMIT_800, LOAN_RATE_800);
        }

        if (esgScore >= 700) {
            return LoanScreeningResult.approve(REASON_AVAILABLE, LOAN_LIMIT_700, LOAN_RATE_700);
        }

        return LoanScreeningResult.reject(REASON_LOW_ESG_SCORE);
    }
}
