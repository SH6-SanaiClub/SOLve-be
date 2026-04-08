package com.shinhan.esg_be.domain.bank.dto.response;

import java.math.BigDecimal;

public record LoanPreviewResponse(
        Long productId,
        String name,
        String subtitle,
        String description,
        boolean available,
        String reason,
        Long loanLimit,
        BigDecimal appliedRate,
        Integer durationMonths,
        Integer baseScore,
        boolean hasActiveLoan,
        boolean loanBlocked
) {
}
