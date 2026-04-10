package com.shinhan.esg_be.domain.bank.dto.response;

import java.time.LocalDateTime;

public record LoanHistoryResponse(
        Long historyId,
        Long loanId,
        Long productId,
        String productName,
        Long amount,
        LocalDateTime paymentDate
) {
}
