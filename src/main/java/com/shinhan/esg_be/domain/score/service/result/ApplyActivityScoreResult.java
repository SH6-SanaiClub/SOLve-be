package com.shinhan.esg_be.domain.score.service.result;

import com.shinhan.esg_be.global.common.enums.ScoreCategory;

public record ApplyActivityScoreResult(
        ScoreCategory scoreCategory,
        int appliedScore,
        int scoreAfter,
        int monthlyScoreAfter,
        boolean cappedByMonthlyLimit
) {
}
