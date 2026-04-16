package com.shinhan.esg_be.domain.bank.entity;

import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "financial_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fin_product_id")
    private Long finProductId;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String subtitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type;

    @Column(name = "base_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "max_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxRate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths = 12;

    @Column(name = "monthly_payment_amount")
    private Long monthlyPaymentAmount;

    public static FinancialProduct create(
            String name,
            String subtitle,
            ProductType type,
            BigDecimal baseRate,
            BigDecimal maxRate,
            String description,
            Integer durationMonths,
            Long monthlyPaymentAmount,
            Boolean isActive
    ) {
        FinancialProduct financialProduct = new FinancialProduct();
        financialProduct.name = name;
        financialProduct.subtitle = subtitle;
        financialProduct.type = type;
        financialProduct.baseRate = baseRate;
        financialProduct.maxRate = maxRate;
        financialProduct.description = description;
        financialProduct.durationMonths = durationMonths;
        financialProduct.monthlyPaymentAmount = monthlyPaymentAmount;
        financialProduct.isActive = isActive;
        return financialProduct;
    }

    public void update(
            String name,
            String subtitle,
            ProductType type,
            BigDecimal baseRate,
            BigDecimal maxRate,
            String description,
            Integer durationMonths,
            Long monthlyPaymentAmount
    ) {
        this.name = name;
        this.subtitle = subtitle;
        this.type = type;
        this.baseRate = baseRate;
        this.maxRate = maxRate;
        this.description = description;
        this.durationMonths = durationMonths;
        this.monthlyPaymentAmount = monthlyPaymentAmount;
    }

    public void updateStatus(boolean isActive) {
        this.isActive = isActive;
    }
}
