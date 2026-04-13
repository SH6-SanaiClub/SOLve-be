package com.shinhan.esg_be.domain.activitystatus.dto.response;

public record ActivityStatusMonthlyScorePointResponse(
        Integer year,
        Integer month,
        Integer score,
        Boolean currentMonth
) {
}
