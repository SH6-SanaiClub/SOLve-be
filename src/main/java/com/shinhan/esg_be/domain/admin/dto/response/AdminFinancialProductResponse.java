package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminFinancialProductResponse {

    private Long finProductId;
    private String name;
    private String subtitle;
    private String type;
    private BigDecimal baseRate;
    private BigDecimal maxRate;
    private Integer durationMonths;
    private Long monthlyPaymentAmount;
    private Boolean isActive;
    private Long activeLoanCount;
    private Long activeSavingCount;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminFinancialProductResponse from(
            FinancialProduct financialProduct,
            long activeLoanCount,
            long activeSavingCount
    ) {
        return new AdminFinancialProductResponse(
                financialProduct.getFinProductId(),
                financialProduct.getName(),
                financialProduct.getSubtitle(),
                financialProduct.getType().name(),
                financialProduct.getBaseRate(),
                financialProduct.getMaxRate(),
                financialProduct.getDurationMonths(),
                financialProduct.getMonthlyPaymentAmount(),
                financialProduct.getIsActive(),
                activeLoanCount,
                activeSavingCount,
                financialProduct.getDescription(),
                financialProduct.getCreatedAt(),
                financialProduct.getUpdatedAt()
        );
    }
}
