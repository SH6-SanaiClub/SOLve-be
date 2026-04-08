package com.shinhan.esg_be.domain.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentPrepareResponse {

    private final String merchantUid;
    private final Long donationId;
    private final String donationName;
    private final Long amount;
}
