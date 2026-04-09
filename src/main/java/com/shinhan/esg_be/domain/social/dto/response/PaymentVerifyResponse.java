package com.shinhan.esg_be.domain.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentVerifyResponse {

    private final Long paymentId;
    private final Long donationId;
    private final String donationName;
    private final Long amount;
    private final String paymentStatus;
    private final Integer awardedPoint;
    private final Integer currentPoint;
}
