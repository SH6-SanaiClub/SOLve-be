package com.shinhan.esg_be.domain.score.service.result;

import com.shinhan.esg_be.domain.score.entity.enums.ScoreCategory;

public record ScoreCalculationResult(
        ScoreCategory scoreCategory,
        int appliedScore,
        int newScore,
        int appliedPoint,
        int monthlyScoreAfter,
        boolean cappedByMonthlyLimit
) {
}
