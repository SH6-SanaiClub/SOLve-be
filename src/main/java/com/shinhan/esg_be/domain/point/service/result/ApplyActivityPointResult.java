package com.shinhan.esg_be.domain.point.service.result;

import com.shinhan.esg_be.domain.point.entity.enums.PointCategory;

public record ApplyActivityPointResult(
        PointCategory pointCategory,
        int activityPoint,
        int bonusPoint,
        int pointAfter,
        boolean blockedByDailyLimit
) {
}
