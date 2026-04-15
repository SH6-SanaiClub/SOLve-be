package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreRecalculationService {

    private final UserRepository userRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;
    private final EsgScorePolicyRepository esgScorePolicyRepository;

    @Transactional
    public void recalculateUserScore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        recalculateUserScore(user);
    }

    @Transactional
    public void recalculateUserScore(User user) {
        List<ValidScoreHistory> histories = validScoreHistoryRepository.findByUser(user);
        Map<ScoreCategory, EsgScorePolicy> policies = loadPolicies();

        Map<ScoreCategory, Integer> scores = new EnumMap<>(ScoreCategory.class);
        for (ScoreCategory category : ScoreCategory.values()) {
            scores.put(category, resolveBaseScore(user, histories, policies.get(category), category));
        }

        Map<ScoreCategory, Integer> scoreFloors = applyAbuseHistories(histories, scores);
        applyNoActivityHistories(histories, scores, scoreFloors);
        applyMonthlyCappedActivityHistories(histories, scores, policies);
        applyUncappedPositiveHistories(histories, scores);
        applyCategoryMaximum(scores, policies);

        user.replaceScores(
                scores.get(ScoreCategory.E),
                scores.get(ScoreCategory.S),
                scores.get(ScoreCategory.G_ACTIVITY),
                scores.get(ScoreCategory.G_REPAYMENT)
        );
    }

    private Map<ScoreCategory, EsgScorePolicy> loadPolicies() {
        Map<ScoreCategory, EsgScorePolicy> policies = new EnumMap<>(ScoreCategory.class);
        for (ScoreCategory category : ScoreCategory.values()) {
            esgScorePolicyRepository.findByCategoryAndIsActiveTrue(category)
                    .ifPresent(policy -> policies.put(category, policy));
        }
        return policies;
    }

    private int resolveBaseScore(
            User user,
            List<ValidScoreHistory> histories,
            EsgScorePolicy policy,
            ScoreCategory category
    ) {
        int historyBaseScore = histories.stream()
                .filter(history -> history.getReason() == ScoreReason.INITIAL_SCORE)
                .filter(history -> history.getCategory() == category)
                .mapToInt(ValidScoreHistory::getChangeAmount)
                .sum();

        if (historyBaseScore != 0) {
            return historyBaseScore;
        }
        if (policy == null) {
            return user.getScore(category);
        }
        return defaultIfNull(policy.getBaseScore());
    }

    private Map<ScoreCategory, Integer> applyAbuseHistories(
            List<ValidScoreHistory> histories,
            Map<ScoreCategory, Integer> scores
    ) {
        histories.stream()
                .filter(history -> history.getReason() == ScoreReason.ABUSE)
                .forEach(history -> scores.compute(
                        history.getCategory(),
                        (category, score) -> Math.max(0, defaultIfNull(score) + history.getChangeAmount())
                ));

        Map<ScoreCategory, Integer> scoreFloors = new EnumMap<>(ScoreCategory.class);
        for (ScoreCategory category : ScoreCategory.values()) {
            scoreFloors.put(category, defaultIfNull(scores.get(category)));
        }
        return scoreFloors;
    }

    private void applyNoActivityHistories(
            List<ValidScoreHistory> histories,
            Map<ScoreCategory, Integer> scores,
            Map<ScoreCategory, Integer> scoreFloors
    ) {
        histories.stream()
                .filter(history -> history.getReason() == ScoreReason.NO_ACTIVITY)
                .forEach(history -> scores.compute(
                        history.getCategory(),
                        (category, score) -> Math.max(
                                scoreFloors.getOrDefault(category, 0),
                                defaultIfNull(score) + history.getChangeAmount()
                        )
                ));
    }

    private void applyMonthlyCappedActivityHistories(
            List<ValidScoreHistory> histories,
            Map<ScoreCategory, Integer> scores,
            Map<ScoreCategory, EsgScorePolicy> policies
    ) {
        Map<ScoreCategory, Map<YearMonth, Integer>> monthlyScores = new EnumMap<>(ScoreCategory.class);

        histories.stream()
                .filter(this::isMonthlyCappedActivityReason)
                .sorted(Comparator
                        .comparing(ValidScoreHistory::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ValidScoreHistory::getScoreId))
                .forEach(history -> {
                    ScoreCategory category = history.getCategory();
                    YearMonth yearMonth = resolveHistoryYearMonth(history);
                    EsgScorePolicy policy = policies.get(category);
                    int monthlyMaxScore = policy == null
                            ? Integer.MAX_VALUE
                            : defaultIfNull(policy.getMonthlyMaxScore());
                    int monthlyCurrentScore = monthlyScores
                            .computeIfAbsent(category, ignored -> new java.util.HashMap<>())
                            .getOrDefault(yearMonth, 0);
                    int remainingMonthlyScore = Math.max(0, monthlyMaxScore - monthlyCurrentScore);
                    int appliedScore = Math.min(Math.max(0, history.getChangeAmount()), remainingMonthlyScore);

                    monthlyScores.get(category).put(yearMonth, monthlyCurrentScore + appliedScore);
                    scores.compute(category, (ignored, score) -> defaultIfNull(score) + appliedScore);
                });
    }

    private void applyUncappedPositiveHistories(List<ValidScoreHistory> histories, Map<ScoreCategory, Integer> scores) {
        histories.stream()
                .filter(this::isUncappedPositiveReason)
                .forEach(history -> scores.compute(
                        history.getCategory(),
                        (category, score) -> defaultIfNull(score) + Math.max(0, history.getChangeAmount())
                ));
    }

    private void applyCategoryMaximum(
            Map<ScoreCategory, Integer> scores,
            Map<ScoreCategory, EsgScorePolicy> policies
    ) {
        for (ScoreCategory category : ScoreCategory.values()) {
            EsgScorePolicy policy = policies.get(category);
            int maxScore = policy == null ? Integer.MAX_VALUE : defaultIfNull(policy.getMaxScore());
            scores.compute(category, (ignored, score) -> Math.min(maxScore, Math.max(0, defaultIfNull(score))));
        }
    }

    private boolean isMonthlyCappedActivityReason(ValidScoreHistory history) {
        return switch (history.getReason()) {
            case DONATION, VOLUNTEER, PURCHASE, QUIZ, PHOTO -> true;
            default -> false;
        };
    }

    private boolean isUncappedPositiveReason(ValidScoreHistory history) {
        return history.getReason() == ScoreReason.LOAN_REPAY
                || history.getReason() == ScoreReason.CONSECUTIVE_BONUS;
    }

    private YearMonth resolveHistoryYearMonth(ValidScoreHistory history) {
        if (history.getCreatedAt() == null) {
            return YearMonth.now();
        }
        return YearMonth.from(history.getCreatedAt());
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
