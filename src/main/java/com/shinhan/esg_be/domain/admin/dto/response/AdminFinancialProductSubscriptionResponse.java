package com.shinhan.esg_be.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminFinancialProductSubscriptionResponse {

    private Long userId;
    private String loginId;
    private String name;
    private String type;
    private Long amount;
    private BigDecimal currentRate;
    private String status;
    private LocalDateTime startDate;
    private LocalDate maturityDate;
    private LocalDate nextRepaymentDate;
}
