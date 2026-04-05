package com.shinhan.esg_be.domain.score.service.command;

import com.shinhan.esg_be.domain.score.entity.enums.ScoreCategory;

import java.math.BigDecimal;

public record ScoreCalculationCommand(
        ScoreCategory scoreCategory,
        int currentScore,
        int scoreValue,
        int monthlyCurrentScore,
        int monthlyMaxScore,
        boolean applyMonthlyCap,
        Integer fixedPointValue,
        BigDecimal pointRate,
        BigDecimal amount
) {
}
