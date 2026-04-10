package com.shinhan.esg_be.domain.bank.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActiveSavingResponse(
        Long savingId,
        Long productId,
        String productName,
        Long monthlyAmount,
        Long paidAmount,
        Long paymentCount,
        Long remainingCount,
        String status,
        Integer durationMonths,
        Boolean hasPenalty,
        LocalDate maturityDate,
        LocalDateTime joinedAt
) {
}
