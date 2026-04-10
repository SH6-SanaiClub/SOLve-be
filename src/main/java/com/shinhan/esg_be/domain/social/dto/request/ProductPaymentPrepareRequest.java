package com.shinhan.esg_be.domain.social.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductPaymentPrepareRequest {

    @NotNull
    private Long productId;
}
