package com.shinhan.esg_be.domain.point.service.result;

import com.shinhan.esg_be.domain.point.entity.enums.PointReason;

public record ApplyActivityPointResult(
        PointReason pointReason,
        int activityPoint,
        int bonusPoint,
        int pointAfter,
        boolean blockedByDailyLimit
) {
}
