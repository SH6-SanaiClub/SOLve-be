package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.policy.entity.PenaltyPolicy;
import com.shinhan.esg_be.domain.policy.entity.enums.PenaltyType;
import com.shinhan.esg_be.domain.policy.repository.PenaltyPolicyRepository;
import com.shinhan.esg_be.domain.recommendation.service.RecommendCacheService;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.score.service.result.PenaltyApplicationResult;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
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
public class ScorePenaltyService {

    private static final List<ScoreReason> SCORE_FLOOR_REASONS = List.of(
            ScoreReason.INITIAL_SCORE,
            ScoreReason.ABUSE
    );

    private final UserRepository userRepository;
    private final PenaltyPolicyRepository penaltyPolicyRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;
    private final RecommendCacheService recommendCacheService;

    @Transactional
    public PenaltyApplicationResult applyAbusePenalty(Long userId, LocalDateTime penalizedAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        PenaltyPolicy penaltyPolicy = penaltyPolicyRepository.findByPenaltyType(PenaltyType.ABUSE)
                .orElseThrow(() -> new IllegalArgumentException("Penalty policy not found."));

        LocalDateTime baseDateTime = resolveBaseDateTime(penalizedAt);
        int eReduction = defaultIfNull(penaltyPolicy.getEReduction());
        int sReduction = defaultIfNull(penaltyPolicy.getSReduction());
        int gReduction = defaultIfNull(penaltyPolicy.getGReduction());

        applyPenaltyHistory(user, ScoreCategory.E, eReduction, ScoreReason.ABUSE, baseDateTime);
        applyPenaltyHistory(user, ScoreCategory.S, sReduction, ScoreReason.ABUSE, baseDateTime);
        applyPenaltyHistory(user, ScoreCategory.G_ACTIVITY, gReduction, ScoreReason.ABUSE, baseDateTime);

        user.increaseAbuseCount();
        if (Boolean.TRUE.equals(penaltyPolicy.getIsBlockLoan())) {
            user.blockLoan();
        }

        recommendCacheService.evictActivityRecommend(userId);

        return new PenaltyApplicationResult(
                ScoreReason.ABUSE,
                eReduction,
                sReduction,
                gReduction,
                user.getIsLoanBlocked(),
                true
        );
    }

    @Transactional
    public PenaltyApplicationResult applyNoActivityPenalty(Long userId, LocalDateTime penalizedAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        PenaltyPolicy penaltyPolicy = penaltyPolicyRepository.findByPenaltyType(PenaltyType.NO_ACTIVITY)
                .orElseThrow(() -> new IllegalArgumentException("Penalty policy not found."));

        LocalDateTime baseDateTime = resolveBaseDateTime(penalizedAt);
        if (!isInactiveForOneMonth(user, baseDateTime) || alreadyAppliedThisMonth(user, baseDateTime)) {
            return new PenaltyApplicationResult(
                    ScoreReason.NO_ACTIVITY,
                    0,
                    0,
                    0,
                    user.getIsLoanBlocked(),
                    false
            );
        }

        int eReduction = calculateNoActivityReduction(user, ScoreCategory.E, defaultIfNull(penaltyPolicy.getEReduction()));
        int sReduction = calculateNoActivityReduction(user, ScoreCategory.S, defaultIfNull(penaltyPolicy.getSReduction()));
        int gReduction = calculateNoActivityReduction(user, ScoreCategory.G_ACTIVITY, defaultIfNull(penaltyPolicy.getGReduction()));

        applyPenaltyHistory(user, ScoreCategory.E, eReduction, ScoreReason.NO_ACTIVITY, baseDateTime);
        applyPenaltyHistory(user, ScoreCategory.S, sReduction, ScoreReason.NO_ACTIVITY, baseDateTime);
        applyPenaltyHistory(user, ScoreCategory.G_ACTIVITY, gReduction, ScoreReason.NO_ACTIVITY, baseDateTime);

        PenaltyApplicationResult result = new PenaltyApplicationResult(
                ScoreReason.NO_ACTIVITY,
                eReduction,
                sReduction,
                gReduction,
                user.getIsLoanBlocked(),
                eReduction > 0 || sReduction > 0 || gReduction > 0
        );

        if (result.applied()) {
            recommendCacheService.evictActivityRecommend(userId);
        }

        return result;
    }

    private boolean isInactiveForOneMonth(User user, LocalDateTime baseDateTime) {
        LocalDateTime referenceDateTime = user.getLastActivityDate() == null ? user.getCreatedAt() : user.getLastActivityDate();
        return referenceDateTime.isBefore(baseDateTime.minusMonths(1));
    }

    private boolean alreadyAppliedThisMonth(User user, LocalDateTime baseDateTime) {
        LocalDateTime monthStart = baseDateTime.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);
        return validScoreHistoryRepository.existsByUserAndReasonAndCreatedAtBetween(
                user,
                ScoreReason.NO_ACTIVITY,
                monthStart,
                nextMonthStart
        );
    }

    private int calculateNoActivityReduction(User user, ScoreCategory scoreCategory, int requestedReduction) {
        if (requestedReduction <= 0) {
            return 0;
        }

        int currentScore = user.getScore(scoreCategory);
        int scoreFloor = calculateScoreFloor(user, scoreCategory);
        return Math.max(0, Math.min(requestedReduction, currentScore - scoreFloor));
    }

    private int calculateScoreFloor(User user, ScoreCategory scoreCategory) {
        return validScoreHistoryRepository.findByUserAndReasonIn(user, SCORE_FLOOR_REASONS)
                .stream()
                .filter(history -> history.getCategory() == scoreCategory)
                .mapToInt(ValidScoreHistory::getChangeAmount)
                .sum();
    }

    private void applyPenaltyHistory(
            User user,
            ScoreCategory scoreCategory,
            int reduction,
            ScoreReason scoreReason,
            LocalDateTime baseDateTime
    ) {
        if (reduction <= 0) {
            return;
        }

        user.applyScore(scoreCategory, -reduction);
        validScoreHistoryRepository.save(
                ValidScoreHistory.create(
                        user,
                        scoreCategory,
                        -reduction,
                        scoreReason,
                        baseDateTime.plusYears(100),
                        user.getScore(scoreCategory)
                )
        );
    }

    private LocalDateTime resolveBaseDateTime(LocalDateTime penalizedAt) {
        return penalizedAt == null ? LocalDateTime.now() : penalizedAt;
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
