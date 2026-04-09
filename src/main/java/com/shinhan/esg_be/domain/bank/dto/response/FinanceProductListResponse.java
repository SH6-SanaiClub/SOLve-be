package com.shinhan.esg_be.domain.bank.dto.response;

import java.util.List;

public record FinanceProductListResponse(
        List<FinanceProductResponse> products
) {
}
