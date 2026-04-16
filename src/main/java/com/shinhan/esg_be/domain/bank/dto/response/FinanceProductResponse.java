package com.shinhan.esg_be.domain.bank.dto.response;

import java.math.BigDecimal;

public record FinanceProductResponse(
        Long id,
        String name,
        String subtitle,
        String type,
        BigDecimal baseRate,
        BigDecimal maxRate,
        BigDecimal appliedRate,
        Long loanLimit,
        boolean available,
        String unavailableReason,
        Integer durationMonths,
        Long monthlyPaymentAmount,
        String description
) {
}
