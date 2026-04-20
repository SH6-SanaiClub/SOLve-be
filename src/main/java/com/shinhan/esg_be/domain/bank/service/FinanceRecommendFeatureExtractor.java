package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.entity.UserScoreSnapshot;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.stat.repository.UserScoreSnapshotRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FinanceRecommendFeatureExtractor {

    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final UserScoreSnapshotRepository userScoreSnapshotRepository;
    private final EsgScorePolicyRepository esgScorePolicyRepository;
    private final Clock clock;

    public FinanceRecommendFeature extract(User user) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime threeMonthsAgo = today.minusMonths(3).atStartOfDay();
        LocalDate snapshotThreshold = today.minusMonths(3);

        Map<ScoreCategory, Integer> monthlyTargets = esgScorePolicyRepository.findAllByIsActiveTrue().stream()
                .collect(Collectors.toMap(
                        EsgScorePolicy::getCategory,
                        policy -> defaultIfNull(policy.getMonthlyMaxScore()),
                        (left, right) -> left
                ));

        List<UserMonthlyStat> recentStats = userMonthlyStatRepository.findTop3ByUser_UserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .filter(stat -> stat.getCreatedAt() != null && !stat.getCreatedAt().isBefore(threeMonthsAgo))
                .sorted(Comparator.comparing(UserMonthlyStat::getCreatedAt).reversed())
                .toList();

        UserMonthlyStat latest = recentStats.isEmpty() ? null : recentStats.get(0);
        int eTarget = monthlyTargets.getOrDefault(ScoreCategory.E, 0);
        int sTarget = monthlyTargets.getOrDefault(ScoreCategory.S, 0);
        int gTarget = monthlyTargets.getOrDefault(ScoreCategory.G_ACTIVITY, 0);

        List<UserScoreSnapshot> recentSnapshots = userScoreSnapshotRepository.findByUser_UserIdOrderBySnapshotDateDesc(user.getUserId())
                .stream()
                .filter(snapshot -> !snapshot.getSnapshotDate().isBefore(snapshotThreshold))
                .sorted(Comparator.comparing(UserScoreSnapshot::getSnapshotDate))
                .toList();

        int quarterGrowth = recentSnapshots.isEmpty()
                ? 0
                : Math.max(user.getTotalScore() - recentSnapshots.get(0).getTotalScore(), 0);

        return new FinanceRecommendFeature(
                !recentStats.isEmpty(),
                recentStats.size(),
                latest != null ? latest.getMonthlyEScore() : 0,
                latest != null ? latest.getMonthlySScore() : 0,
                latest != null ? latest.getMonthlyGScore() : 0,
                eTarget,
                sTarget,
                gTarget,
                averageRatio(recentStats, CategoryScore::eScore, eTarget),
                averageRatio(recentStats, CategoryScore::sScore, sTarget),
                averageRatio(recentStats, CategoryScore::gScore, gTarget),
                latestRatio(latest != null ? latest.getMonthlyEScore() : 0, eTarget),
                latestRatio(latest != null ? latest.getMonthlySScore() : 0, sTarget),
                latestRatio(latest != null ? latest.getMonthlyGScore() : 0, gTarget),
                countTargetHits(recentStats, CategoryScore::eScore, eTarget),
                countTargetHits(recentStats, CategoryScore::sScore, sTarget),
                countTargetHits(recentStats, CategoryScore::gScore, gTarget),
                latest != null ? latest.getConsecutiveEMaxScore() : 0,
                latest != null ? latest.getConsecutiveSMaxScore() : 0,
                latest != null ? latest.getConsecutiveGMaxScore() : 0,
                averageMonthlyTotalScore(recentStats),
                activeCategoryCount(recentStats),
                user.getTotalScore(),
                quarterGrowth,
                recentSnapshots.size(),
                (int) recentSnapshots.stream()
                        .filter(snapshot -> snapshot.getTotalScore() >= SavingsProductKind.ESG_MASTER_MIN_TOTAL_SCORE)
                        .count()
        );
    }

    private double averageRatio(List<UserMonthlyStat> stats, CategoryScore scoreExtractor, int target) {
        if (stats.isEmpty() || target <= 0) {
            return 0.0;
        }

        return stats.stream()
                .mapToDouble(stat -> latestRatio(scoreExtractor.extract(stat), target))
                .average()
                .orElse(0.0);
    }

    private int countTargetHits(List<UserMonthlyStat> stats, CategoryScore scoreExtractor, int target) {
        if (target <= 0) {
            return 0;
        }

        return (int) stats.stream()
                .filter(stat -> scoreExtractor.extract(stat) >= target)
                .count();
    }

    private int averageMonthlyTotalScore(List<UserMonthlyStat> stats) {
        if (stats.isEmpty()) {
            return 0;
        }

        return (int) Math.round(stats.stream()
                .mapToInt(stat -> stat.getMonthlyEScore() + stat.getMonthlySScore() + stat.getMonthlyGScore())
                .average()
                .orElse(0.0));
    }

    private int activeCategoryCount(List<UserMonthlyStat> stats) {
        if (stats.isEmpty()) {
            return 0;
        }

        boolean hasE = stats.stream().anyMatch(stat -> stat.getMonthlyEScore() > 0);
        boolean hasS = stats.stream().anyMatch(stat -> stat.getMonthlySScore() > 0);
        boolean hasG = stats.stream().anyMatch(stat -> stat.getMonthlyGScore() > 0);

        return (hasE ? 1 : 0) + (hasS ? 1 : 0) + (hasG ? 1 : 0);
    }

    private double latestRatio(int score, int target) {
        if (target <= 0) {
            return 0.0;
        }
        return Math.min((double) score / target, 1.0);
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    @FunctionalInterface
    private interface CategoryScore {
        int extract(UserMonthlyStat stat);

        static int eScore(UserMonthlyStat stat) {
            return stat.getMonthlyEScore();
        }

        static int sScore(UserMonthlyStat stat) {
            return stat.getMonthlySScore();
        }

        static int gScore(UserMonthlyStat stat) {
            return stat.getMonthlyGScore();
        }
    }
}
