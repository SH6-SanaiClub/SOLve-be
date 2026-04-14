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
import com.shinhan.esg_be.global.common.enums.ActivityType;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreService {

    private static final List<ScoreCategory> INITIAL_SCORE_CATEGORIES = List.of(
            ScoreCategory.E,
            ScoreCategory.S,
            ScoreCategory.G_ACTIVITY,
            ScoreCategory.G_REPAYMENT
    );

    private final UserRepository userRepository;
    private final ActivityRewardPolicyRepository activityRewardPolicyRepository;
    private final EsgScorePolicyRepository esgScorePolicyRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;
    private final ScoreCalculatorService scoreCalculatorService;

    @Transactional
    public void initializeUserScore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (validScoreHistoryRepository.existsByUserAndReason(user, ScoreReason.INITIAL_SCORE)) {
            throw new IllegalStateException("Initial score already initialized.");
        }

        userMonthlyStatRepository.findByUser(user)
                .orElseGet(() -> userMonthlyStatRepository.save(UserMonthlyStat.create(user)));

        LocalDateTime initializedAt = LocalDateTime.now();

        for (ScoreCategory scoreCategory : INITIAL_SCORE_CATEGORIES) {
            EsgScorePolicy esgScorePolicy = esgScorePolicyRepository.findByCategoryAndIsActiveTrue(scoreCategory)
                    .orElseThrow(() -> new IllegalArgumentException("ESG score policy not found."));

            int baseScore = defaultIfNull(esgScorePolicy.getBaseScore());
            user.initializeScore(scoreCategory, baseScore);

            validScoreHistoryRepository.save(
                    ValidScoreHistory.create(
                            user,
                            scoreCategory,
                            baseScore,
                            ScoreReason.INITIAL_SCORE,
                            initializedAt.plusYears(100),
                            user.getScore(scoreCategory)
                    )
            );
        }
    }

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

        LocalDateTime activityDateTime = resolveActivityDateTime(command.activityDateTime());
        ScoreReason scoreReason = mapToScoreReason(command.activityType());
        if (isDailyScoreLimited(command.activityType()) && alreadyAppliedDailyScore(user, scoreReason, activityDateTime)) {
            return new ApplyActivityScoreResult(
                    scoreCategory,
                    0,
                    user.getScore(scoreCategory),
                    userMonthlyStat.getMonthlyScore(scoreCategory),
                    false
            );
        }

        ScoreCalculationResult calculationResult = scoreCalculatorService.calculateActivity(
                new ScoreCalculationCommand(
                        scoreCategory,
                        user.getScore(scoreCategory),
                        defaultIfNull(esgScorePolicy.getMaxScore()),
                        defaultIfNull(activityRewardPolicy.getScoreValue()),
                        userMonthlyStat.getMonthlyScore(scoreCategory),
                        defaultIfNull(esgScorePolicy.getMonthlyMaxScore()),
                        isMonthlyCapTarget(scoreCategory)
                )
        );

        if (calculationResult.appliedScore() > 0) {
            user.applyScore(scoreCategory, calculationResult.appliedScore());
            user.updateLastActivityDate(activityDateTime);
            userMonthlyStat.addScore(scoreCategory, calculationResult.appliedScore());

            validScoreHistoryRepository.save(
                    ValidScoreHistory.create(
                            user,
                            scoreCategory,
                            calculationResult.appliedScore(),
                            scoreReason,
                            activityDateTime.plusYears(1),
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

    private boolean isDailyScoreLimited(ActivityType activityType) {
        return switch (activityType) {
            case DONATION, VOLUNTEER, PURCHASE, QUIZ_CORRECT, QUIZ_WRONG, PHOTO -> true;
            case LOAN_REPAY -> false;
        };
    }

    private boolean alreadyAppliedDailyScore(User user, ScoreReason scoreReason, LocalDateTime activityDateTime) {
        LocalDateTime startOfDay = activityDateTime.toLocalDate().atStartOfDay();
        LocalDateTime nextDay = startOfDay.plusDays(1);
        return validScoreHistoryRepository.existsByUserAndReasonAndCreatedAtBetween(
                user,
                scoreReason,
                startOfDay,
                nextDay
        );
    }

    private ScoreReason mapToScoreReason(ActivityType activityType) {
        return switch (activityType) {
            case DONATION -> ScoreReason.DONATION;
            case VOLUNTEER -> ScoreReason.VOLUNTEER;
            case PURCHASE -> ScoreReason.PURCHASE;
            case QUIZ_CORRECT, QUIZ_WRONG -> ScoreReason.QUIZ;
            case PHOTO -> ScoreReason.PHOTO;
            case LOAN_REPAY -> ScoreReason.LOAN_REPAY;
        };
    }
}
