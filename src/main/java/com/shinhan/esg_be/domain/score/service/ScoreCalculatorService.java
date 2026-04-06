package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.score.service.command.ScoreCalculationCommand;
import com.shinhan.esg_be.domain.score.service.result.ScoreCalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ScoreCalculatorService {

    // 월 한도와 포인트 정책을 반영해 점수 계산 결과를 만든다.
    public ScoreCalculationResult calculateActivity(ScoreCalculationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("ScoreCalculationCommand must not be null.");
        }

        int appliedScore = command.scoreValue();
        boolean isCapped = false;

        if (command.applyMonthlyCap() && command.scoreValue() > 0) {
            int remaining = Math.max(0, command.monthlyMaxScore() - command.monthlyCurrentScore());
            if (command.scoreValue() > remaining) {
                appliedScore = remaining;
                isCapped = true;
            }
        }

        int newScore = command.currentScore() + appliedScore;
        int appliedPoint = calculatePoint(command.fixedPointValue(), command.pointRate(), command.amount());
        int monthlyScoreAfter = command.monthlyCurrentScore() + appliedScore;

        return new ScoreCalculationResult(
                command.scoreCategory(),
                appliedScore,
                newScore,
                appliedPoint,
                monthlyScoreAfter,
                isCapped
        );
    }

    // 고정 포인트가 있으면 우선 적용하고, 없으면 비율 기반으로 계산한다.
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
