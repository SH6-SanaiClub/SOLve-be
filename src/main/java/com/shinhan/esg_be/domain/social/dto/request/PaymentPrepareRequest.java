package com.shinhan.esg_be.domain.social.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentPrepareRequest {

    @NotNull
    private Long donationId;

    @NotNull
    @Min(30000)
    private Long amount;
}
