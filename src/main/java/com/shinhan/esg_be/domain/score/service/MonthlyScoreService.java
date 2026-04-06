package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.score.service.result.MonthlyScoreSettlementResult;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
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
public class MonthlyScoreService {

    private static final List<ScoreCategory> MONTHLY_TARGET_CATEGORIES = List.of(
            ScoreCategory.E,
            ScoreCategory.S,
            ScoreCategory.G_ACTIVITY
    );

    private final UserRepository userRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final EsgScorePolicyRepository esgScorePolicyRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;

    @Transactional
    public MonthlyScoreSettlementResult settleMonthlyScore(Long userId, LocalDateTime settledAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        UserMonthlyStat userMonthlyStat = userMonthlyStatRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("User monthly stat not found."));

        LocalDateTime baseDateTime = settledAt == null ? LocalDateTime.now() : settledAt;
        int bonusCount = 0;

        // 월 누적 점수를 기준으로 연속 달성 개월 수와 보너스 지급 여부를 계산한다.
        for (ScoreCategory scoreCategory : MONTHLY_TARGET_CATEGORIES) {
            EsgScorePolicy esgScorePolicy = esgScorePolicyRepository.findByCategoryAndIsActiveTrue(scoreCategory)
                    .orElseThrow(() -> new IllegalArgumentException("ESG score policy not found."));

            int nextConsecutiveCount = calculateNextConsecutiveCount(
                    scoreCategory,
                    userMonthlyStat,
                    defaultIfNull(esgScorePolicy.getMonthlyMaxScore())
            );

            updateConsecutiveCount(scoreCategory, userMonthlyStat, nextConsecutiveCount);

            if (nextConsecutiveCount < defaultIfNull(esgScorePolicy.getConsecutiveTargetMonths())) {
                continue;
            }

            int bonusScore = defaultIfNull(esgScorePolicy.getConsecutiveBonusScore());
            if (bonusScore <= 0) {
                continue;
            }

            user.applyScore(scoreCategory, bonusScore);
            validScoreHistoryRepository.save(
                    ValidScoreHistory.create(
                            user,
                            scoreCategory,
                            bonusScore,
                            ScoreReason.CONSECUTIVE_BONUS,
                            baseDateTime.plusYears(1),
                            user.getScore(scoreCategory)
                    )
            );

            updateConsecutiveCount(scoreCategory, userMonthlyStat, 0);
            bonusCount += 1;
        }

        // 월 마감이 끝나면 다음 달 집계를 위해 월 누적 점수를 초기화한다.
        userMonthlyStat.resetMonthlyScores();
        return new MonthlyScoreSettlementResult(bonusCount);
    }

    private int calculateNextConsecutiveCount(
            ScoreCategory scoreCategory,
            UserMonthlyStat userMonthlyStat,
            int monthlyMaxScore
    ) {
        boolean achievedMonthlyMax = userMonthlyStat.getMonthlyScore(scoreCategory) >= monthlyMaxScore;
        if (!achievedMonthlyMax) {
            return 0;
        }

        return switch (scoreCategory) {
            case E -> userMonthlyStat.getConsecutiveEMaxScore() + 1;
            case S -> userMonthlyStat.getConsecutiveSMaxScore() + 1;
            case G_ACTIVITY -> userMonthlyStat.getConsecutiveGMaxScore() + 1;
            case G_REPAYMENT -> 0;
        };
    }

    private void updateConsecutiveCount(
            ScoreCategory scoreCategory,
            UserMonthlyStat userMonthlyStat,
            int consecutiveCount
    ) {
        switch (scoreCategory) {
            case E -> userMonthlyStat.updateEConsecutiveMaxScore(consecutiveCount);
            case S -> userMonthlyStat.updateSConsecutiveMaxScore(consecutiveCount);
            case G_ACTIVITY -> userMonthlyStat.updateGConsecutiveMaxScore(consecutiveCount);
            case G_REPAYMENT -> {
            }
        }
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
