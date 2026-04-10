package com.shinhan.esg_be.domain.bank.dto.response;

import java.util.List;

public record FinanceHistoryResponse(
        List<LoanHistoryResponse> loans,
        List<SavingHistoryResponse> savings
) {
}
