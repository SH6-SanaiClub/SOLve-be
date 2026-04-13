package com.shinhan.esg_be.domain.activitystatus.dto.response;

import com.shinhan.esg_be.domain.user.entity.enums.Grade;

import java.time.LocalDate;

public record ActivityStatusSummaryResponse(
        Grade currentGrade,
        Integer totalScore,
        Integer progressCurrent,
        Integer progressTarget,
        Integer progressPercent,
        Integer currentMonthActivityCount,
        Integer consecutiveMaxAchievementMonths,
        Boolean showConsecutiveAchievementBadge,
        LocalDate referenceDate
) {
}
