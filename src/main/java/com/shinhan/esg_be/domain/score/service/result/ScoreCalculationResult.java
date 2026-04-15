package com.shinhan.esg_be.domain.score.service.result;

import com.shinhan.esg_be.global.common.enums.ScoreCategory;

public record ScoreCalculationResult(
        ScoreCategory scoreCategory,
        int appliedScore,
        int newScore,
        int monthlyScoreAfter,
        boolean cappedByMonthlyLimit,
        int monthlyAppliedScore
) {
}
