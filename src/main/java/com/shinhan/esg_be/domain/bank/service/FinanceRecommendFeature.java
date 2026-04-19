package com.shinhan.esg_be.domain.bank.service;

record FinanceRecommendFeature(
        boolean hasRecentActivity,
        int recentMonthCount,
        int latestEScore,
        int latestSScore,
        int latestGScore,
        int eMonthlyTarget,
        int sMonthlyTarget,
        int gMonthlyTarget,
        double averageERatio,
        double averageSRatio,
        double averageGRatio,
        double latestERatio,
        double latestSRatio,
        double latestGRatio,
        int eTargetHitCount,
        int sTargetHitCount,
        int gTargetHitCount,
        int latestConsecutiveE,
        int latestConsecutiveS,
        int latestConsecutiveG,
        int averageMonthlyTotalScore,
        int activeCategoryCount,
        int currentTotalScore,
        int scoreGrowthLastQuarter,
        int recentSnapshotCount,
        int over900SnapshotCount
) {

    double targetHitRatio(int hitCount) {
        if (recentMonthCount <= 0) {
            return 0.0;
        }
        return (double) hitCount / recentMonthCount;
    }

    double scoreGrowthRatio() {
        return normalize(scoreGrowthLastQuarter, 120);
    }

    double averageTotalActivityRatio() {
        int totalTarget = eMonthlyTarget + sMonthlyTarget + gMonthlyTarget;
        if (totalTarget <= 0) {
            return 0.0;
        }
        return normalize(averageMonthlyTotalScore, totalTarget);
    }

    double activeCategoryRatio() {
        return normalize(activeCategoryCount, 3);
    }

    double recentActivityRatio() {
        return normalize(recentMonthCount, 3);
    }

    double recentOver900Ratio() {
        if (recentSnapshotCount <= 0) {
            return 0.0;
        }
        return (double) over900SnapshotCount / recentSnapshotCount;
    }

    double masterBufferRatio() {
        return normalize(Math.max(currentTotalScore - SavingsProductKind.ESG_MASTER_MIN_TOTAL_SCORE, 0), 60);
    }

    private static double normalize(int value, int max) {
        if (max <= 0) {
            return 0.0;
        }
        return Math.min(Math.max((double) value / max, 0.0), 1.0);
    }
}
