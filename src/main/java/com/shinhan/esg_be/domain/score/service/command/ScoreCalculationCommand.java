package com.shinhan.esg_be.domain.score.service.command;

import com.shinhan.esg_be.global.common.enums.ScoreCategory;

import java.math.BigDecimal;

public record ScoreCalculationCommand(
        ScoreCategory scoreCategory,
        int currentScore,
        int maxScore,
        int scoreValue,
        int monthlyCurrentScore,
        int monthlyMaxScore,
        boolean applyMonthlyCap,
        Integer fixedPointValue,
        BigDecimal pointRate,
        BigDecimal amount
) {
}
