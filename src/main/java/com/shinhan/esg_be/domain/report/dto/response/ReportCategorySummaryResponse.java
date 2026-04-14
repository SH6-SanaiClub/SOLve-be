package com.shinhan.esg_be.domain.report.dto.response;

public record ReportCategorySummaryResponse(
        String categoryKey,
        String label,
        Integer value,
        String unit
) {
}
