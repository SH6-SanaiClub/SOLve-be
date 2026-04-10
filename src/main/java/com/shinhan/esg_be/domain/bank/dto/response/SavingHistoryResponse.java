package com.shinhan.esg_be.domain.bank.dto.response;

import java.time.LocalDateTime;

public record SavingHistoryResponse(
        Long historyId,
        Long savingId,
        Long productId,
        String productName,
        Long amount,
        LocalDateTime paymentDate
) {
}
