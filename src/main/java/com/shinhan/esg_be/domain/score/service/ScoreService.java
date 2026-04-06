package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.policy.entity.ActivityRewardPolicy;
import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.ActivityRewardPolicyRepository;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.score.service.command.ApplyActivityScoreCommand;
import com.shinhan.esg_be.domain.score.service.command.ScoreCalculationCommand;
import com.shinhan.esg_be.domain.score.service.result.ApplyActivityScoreResult;
import com.shinhan.esg_be.domain.score.service.result.ScoreCalculationResult;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreService {

    private final UserRepository userRepository;
    private final ActivityRewardPolicyRepository activityRewardPolicyRepository;
    private final EsgScorePolicyRepository esgScorePolicyRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;
    private final ScoreCalculatorService scoreCalculatorService;

    @Transactional
    public ApplyActivityScoreResult applyActivityScore(ApplyActivityScoreCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        ActivityRewardPolicy activityRewardPolicy = activityRewardPolicyRepository.findByActivityType(command.activityType())
                .orElseThrow(() -> new IllegalArgumentException("Activity reward policy not found."));

        ScoreCategory scoreCategory = activityRewardPolicy.getScoreCategory();
        EsgScorePolicy esgScorePolicy = esgScorePolicyRepository.findByCategoryAndIsActiveTrue(scoreCategory)
                .orElseThrow(() -> new IllegalArgumentException("ESG score policy not found."));

        UserMonthlyStat userMonthlyStat = userMonthlyStatRepository.findByUser(user)
                .orElseGet(() -> userMonthlyStatRepository.save(UserMonthlyStat.create(user)));

        ScoreCalculationResult calculationResult = scoreCalculatorService.calculateActivity(
                new ScoreCalculationCommand(
                        scoreCategory,
                        user.getScore(scoreCategory),
                        defaultIfNull(activityRewardPolicy.getScoreValue()),
                        userMonthlyStat.getMonthlyScore(scoreCategory),
                        defaultIfNull(esgScorePolicy.getMonthlyMaxScore()),
                        isMonthlyCapTarget(scoreCategory),
                        activityRewardPolicy.getPointValue(),
                        activityRewardPolicy.getPointRate(),
                        command.amount()
                )
        );

        if (calculationResult.appliedScore() > 0) {
            user.applyScore(scoreCategory, calculationResult.appliedScore());
            user.updateLastActivityDate(resolveActivityDateTime(command.activityDateTime()));
            userMonthlyStat.addScore(scoreCategory, calculationResult.appliedScore());

            validScoreHistoryRepository.save(
                    ValidScoreHistory.create(
                            user,
                            scoreCategory,
                            calculationResult.appliedScore(),
                            command.activityType(),
                            resolveActivityDateTime(command.activityDateTime()).plusYears(1),
                            calculationResult.newScore()
                    )
            );
        }

        return new ApplyActivityScoreResult(
                scoreCategory,
                calculationResult.appliedScore(),
                calculationResult.newScore(),
                calculationResult.monthlyScoreAfter(),
                calculationResult.cappedByMonthlyLimit()
        );
    }

    private boolean isMonthlyCapTarget(ScoreCategory scoreCategory) {
        return scoreCategory != ScoreCategory.G_REPAYMENT;
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime resolveActivityDateTime(LocalDateTime activityDateTime) {
        return activityDateTime == null ? LocalDateTime.now() : activityDateTime;
    }
}
