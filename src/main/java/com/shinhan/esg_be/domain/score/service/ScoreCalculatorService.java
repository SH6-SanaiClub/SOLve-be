package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.score.service.command.ScoreCalculationCommand;
import com.shinhan.esg_be.domain.score.service.result.ScoreCalculationResult;
import org.springframework.stereotype.Service;

@Service
public class ScoreCalculatorService {

    // 월/전체 상한을 반영한 실제 적립 점수를 계산한다.
    public ScoreCalculationResult calculateActivity(ScoreCalculationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("ScoreCalculationCommand must not be null.");
        }

        int appliedScore = Math.max(0, command.scoreValue());
        boolean isCapped = false;

        if (command.applyMonthlyCap() && appliedScore > 0) {
            int remainingMonthlyScore = Math.max(0, command.monthlyMaxScore() - command.monthlyCurrentScore());
            if (appliedScore > remainingMonthlyScore) {
                appliedScore = remainingMonthlyScore;
                isCapped = true;
            }
        }

        int remainingTotalScore = Math.max(0, command.maxScore() - command.currentScore());
        if (appliedScore > remainingTotalScore) {
            appliedScore = remainingTotalScore;
            isCapped = true;
        }

        return new ScoreCalculationResult(
                command.scoreCategory(),
                appliedScore,
                command.currentScore() + appliedScore,
                command.monthlyCurrentScore() + appliedScore,
                isCapped
        );
    }
}
