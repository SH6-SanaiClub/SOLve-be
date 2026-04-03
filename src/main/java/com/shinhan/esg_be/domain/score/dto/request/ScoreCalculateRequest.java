package com.shinhan.esg_be.domain.score.dto.request;

import com.shinhan.esg_be.domain.score.entity.enums.ScoreCategory;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ScoreCalculateRequest {

    // 점수 계산에 필요한 입력값을 묶는다.
    private final ScoreCategory scoreCategory;
    private final int currentScore;
    private final int scoreValue;
    private final int monthlyCurrentScore;
    private final int monthlyMaxScore;
    private final boolean applyMonthlyCap;
    private final Integer fixedPointValue;
    private final BigDecimal pointRate;
    private final BigDecimal amount;

    public ScoreCalculateRequest(
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
        this.scoreCategory = scoreCategory;
        this.currentScore = currentScore;
        this.scoreValue = scoreValue;
        this.monthlyCurrentScore = monthlyCurrentScore;
        this.monthlyMaxScore = monthlyMaxScore;
        this.applyMonthlyCap = applyMonthlyCap;
        this.fixedPointValue = fixedPointValue;
        this.pointRate = pointRate;
        this.amount = amount;
    }
}
