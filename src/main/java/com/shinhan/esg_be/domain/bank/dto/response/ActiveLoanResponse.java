package com.shinhan.esg_be.domain.bank.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActiveLoanResponse(
        Long loanId,
        Long productId,
        String productName,
        Long principalAmount,
        Long totalAmount,
        Long paidAmount,
        Long remainingAmount,
        Long repaymentCount,
        BigDecimal currentRate,
        String status,
        Integer durationMonths,
        LocalDate nextRepaymentDate,
        LocalDateTime joinedAt
) {
}
