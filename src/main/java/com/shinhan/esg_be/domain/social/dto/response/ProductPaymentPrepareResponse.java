package com.shinhan.esg_be.domain.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductPaymentPrepareResponse {

    private final String merchantUid;
    private final Long productId;
    private final String productName;
    private final String storeName;
    private final String category;
    private final Long amount;
}
