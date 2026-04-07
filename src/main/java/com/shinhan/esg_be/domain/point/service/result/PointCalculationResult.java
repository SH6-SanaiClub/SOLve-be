package com.shinhan.esg_be.domain.point.service.result;

import com.shinhan.esg_be.domain.point.entity.enums.PointReason;

public record PointCalculationResult(
        PointReason pointReason,
        int appliedPoint,
        int pointAfter
) {
}
