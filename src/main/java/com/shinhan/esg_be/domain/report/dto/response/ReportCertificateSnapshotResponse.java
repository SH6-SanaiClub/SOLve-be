package com.shinhan.esg_be.domain.report.dto.response;

import com.shinhan.esg_be.domain.user.entity.enums.Grade;

import java.time.LocalDate;
import java.util.List;

public record ReportCertificateSnapshotResponse(
        String recipientName,
        String periodType,
        String periodLabel,
        LocalDate referenceDate,
        LocalDate targetStartDate,
        LocalDate targetEndDate,
        Grade currentGrade,
        Integer totalScore,
        Integer verifiedActivityCount,
        Integer consecutiveMaxAchievementMonths,
        Boolean showConsecutiveAchievementBadge,
        List<ReportCategorySummaryResponse> categories
) {
}
