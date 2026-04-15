package com.shinhan.esg_be.domain.admin.dto.request;

import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AdminFinancialProductCreateRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    private String subtitle;

    @NotNull(message = "상품 유형은 필수입니다.")
    private ProductType type;

    @NotNull(message = "기본 금리는 필수입니다.")
    @DecimalMin(value = "0.00", message = "기본 금리는 0 이상이어야 합니다.")
    private BigDecimal baseRate;

    @NotNull(message = "최대 금리는 필수입니다.")
    @DecimalMin(value = "0.00", message = "최대 금리는 0 이상이어야 합니다.")
    private BigDecimal maxRate;

    @NotBlank(message = "설명은 필수입니다.")
    private String description;

    @NotNull(message = "가입 기간은 필수입니다.")
    @Positive(message = "가입 기간은 0보다 커야 합니다.")
    private Integer durationMonths;

    @PositiveOrZero(message = "월 납입액은 0 이상이어야 합니다.")
    private Long monthlyPaymentAmount;

    @NotNull(message = "활성 상태는 필수입니다.")
    private Boolean isActive;
}
