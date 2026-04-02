package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.score.dto.request.ScoreCalculateRequest;
import com.shinhan.esg_be.domain.score.dto.response.ScoreCalculateResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ScoreCalculatorService {

    // 월 제한 컷, 점수 합산, 포인트 계산까지 순수 계산을 수행한다.
    // DB 저장이나 사용자 상태 변경은 호출하는 Service에서 처리한다.
    public ScoreCalculateResponse calculateActivity(ScoreCalculateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ScoreCalculateRequest must not be null.");
        }

        // 월 최대치를 넘지 않도록 남은 한도만큼 점수를 반영한다.
        int appliedScore = request.getScoreValue();
        boolean isCapped = false;
        if (request.isApplyMonthlyCap() && request.getScoreValue() > 0) {
            int remaining = Math.max(0, request.getMonthlyMaxScore() - request.getMonthlyCurrentScore());
            if (request.getScoreValue() > remaining) {
                appliedScore = remaining;
                isCapped = true;
            }
        }

        // 최종 점수와 포인트를 계산해 결과에 담는다.
        int newScore = request.getCurrentScore() + appliedScore;
        int appliedPoint = calculatePoint(request.getFixedPointValue(), request.getPointRate(), request.getAmount());
        int monthlyScoreAfter = request.getMonthlyCurrentScore() + appliedScore;

        return new ScoreCalculateResponse(
                request.getScoreCategory(),
                appliedScore,
                newScore,
                appliedPoint,
                monthlyScoreAfter,
                isCapped
        );
    }

    // 포인트는 고정값이 있으면 그대로, 없으면 비율 기반(반올림)으로 산정한다.
    private int calculatePoint(Integer fixedPointValue, BigDecimal pointRate, BigDecimal amount) {
        if (fixedPointValue != null && fixedPointValue > 0) {
            return fixedPointValue;
        }
        if (pointRate != null && amount != null && pointRate.compareTo(BigDecimal.ZERO) > 0) {
            return amount.multiply(pointRate)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
        }
        return 0;
    }
}
