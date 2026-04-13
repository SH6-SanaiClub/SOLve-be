package com.shinhan.esg_be.domain.activitystatus.dto.response;

import com.shinhan.esg_be.domain.user.entity.enums.Grade;

public record ActivityStatusGradeHistoryItemResponse(
        Integer year,
        Integer month,
        Grade grade,
        Integer totalScore,
        Boolean currentMonth
) {
}
