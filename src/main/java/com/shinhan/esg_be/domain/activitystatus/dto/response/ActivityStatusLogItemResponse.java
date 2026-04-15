package com.shinhan.esg_be.domain.activitystatus.dto.response;

import com.shinhan.esg_be.global.common.enums.ScoreReason;

import java.time.LocalDateTime;

public record ActivityStatusLogItemResponse(
        Long scoreHistoryId,
        String title,
        String category,
        ScoreReason reason,
        LocalDateTime occurredAt
) {
}
