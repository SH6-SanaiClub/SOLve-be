package com.shinhan.esg_be.domain.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ProductPaymentVerifyRequest {

    @NotNull
    private Long productId;

    @NotBlank
    private String impUid;

    @NotBlank
    private String merchantUid;

    public Long getProductId() {
        return productId;
    }

    public String getImpUid() {
        return impUid;
    }

    public String getMerchantUid() {
        return merchantUid;
    }
}
