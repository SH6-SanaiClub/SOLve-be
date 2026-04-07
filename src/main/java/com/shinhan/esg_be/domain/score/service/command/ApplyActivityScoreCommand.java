package com.shinhan.esg_be.domain.score.service.command;

import com.shinhan.esg_be.global.common.enums.ActivityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApplyActivityScoreCommand(
        Long userId,
        ActivityType activityType,
        BigDecimal amount,
        LocalDateTime activityDateTime
) {
}
