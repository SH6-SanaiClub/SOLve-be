package com.shinhan.esg_be.domain.bank.dto.request;

import jakarta.validation.constraints.NotNull;

public record SavingApplyRequest(
        @NotNull(message = "적금 상품 ID는 필수입니다.")
        Long productId
) {
}
