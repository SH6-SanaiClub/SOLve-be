package com.shinhan.esg_be.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class AdminDashboardResponse {

    private Long totalUsers;
    private Long todayNewUsers;
    private Long weekNewUsers;
    private Long activeUsers;
    private Map<String, Long> gradeDistribution;
    private Long thisMonthDonationCount;
    private Long thisMonthDonationAmount;
    private Long thisMonthVolunteerCount;
    private Long thisMonthPurchaseCount;
    private Long activeLoanCount;
    private Long activeLoanAmount;
    private Long activeSavingCount;
}
