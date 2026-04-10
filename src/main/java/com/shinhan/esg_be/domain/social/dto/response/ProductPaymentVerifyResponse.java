package com.shinhan.esg_be.domain.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductPaymentVerifyResponse {

    private final Long paymentId;
    private final Long productId;
    private final String productName;
    private final Long amount;
    private final String paymentStatus;
    private final Integer awardedPoint;
    private final Integer currentPoint;
}
