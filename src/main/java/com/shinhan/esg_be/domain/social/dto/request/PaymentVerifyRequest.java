package com.shinhan.esg_be.domain.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentVerifyRequest {

    @NotNull
    private Long donationId;

    @NotBlank
    private String impUid;

    @NotBlank
    private String merchantUid;
}
