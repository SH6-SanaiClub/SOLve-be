package com.shinhan.esg_be.domain.activitystatus.dto.response;

import java.util.List;

public record ActivityStatusOverviewResponse(
        ActivityStatusSummaryResponse summary,
        List<ActivityStatusGradeHistoryItemResponse> gradeHistories,
        List<ActivityStatusMonthlyScorePointResponse> monthlyScoreGraph
) {
}
