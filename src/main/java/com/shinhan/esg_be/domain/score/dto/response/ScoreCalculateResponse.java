package com.shinhan.esg_be.domain.score.dto.response;

import com.shinhan.esg_be.domain.score.entity.enums.ScoreCategory;
import lombok.Getter;

@Getter
public class ScoreCalculateResponse {

    // 계산 결과(반영 점수/포인트/월 누적)를 반환한다.
    private final ScoreCategory scoreCategory;
    private final int appliedScore;
    private final int newScore;
    private final int appliedPoint;
    private final int monthlyScoreAfter;
    private final boolean cappedByMonthlyLimit;

    public ScoreCalculateResponse(
            ScoreCategory scoreCategory,
            int appliedScore,
            int newScore,
            int appliedPoint,
            int monthlyScoreAfter,
            boolean cappedByMonthlyLimit
    ) {
        this.scoreCategory = scoreCategory;
        this.appliedScore = appliedScore;
        this.newScore = newScore;
        this.appliedPoint = appliedPoint;
        this.monthlyScoreAfter = monthlyScoreAfter;
        this.cappedByMonthlyLimit = cappedByMonthlyLimit;
    }
}
