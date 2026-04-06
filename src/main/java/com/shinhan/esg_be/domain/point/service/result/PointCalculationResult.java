package com.shinhan.esg_be.domain.point.service.result;

import com.shinhan.esg_be.domain.point.entity.enums.PointCategory;

public record PointCalculationResult(
        PointCategory pointCategory,
        int appliedPoint,
        int pointAfter
) {
}
