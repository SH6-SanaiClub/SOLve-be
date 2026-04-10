package com.shinhan.esg_be.domain.bank.dto.response;

public record FinanceMyResponse(
        ActiveLoanResponse activeLoan,
        ActiveSavingResponse activeSaving
) {
}
