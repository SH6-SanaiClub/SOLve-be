package com.shinhan.esg_be.domain.bank.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LoanApplyRequest(
        @NotNull(message = "대출 상품 ID는 필수입니다.")
        Long loanId,

        @NotNull(message = "대출 금액은 필수입니다.")
        @Positive(message = "대출 금액은 0보다 커야 합니다.")
        Long amount
) {
}
