package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SavingsProductScorer {

    public ScoredProduct score(FinancialProduct product, FinanceRecommendFeature feature) {
        SavingsProductKind kind = SavingsProductKind.from(product).orElse(null);
        if (kind == null) {
            return new ScoredProduct(
                    product,
                    0,
                    formatRateLabel(product),
                    "현재 맞춤 규칙과 직접 연결된 상품 정보가 부족해 기본 적금 후보로 안내해드려요.",
                    "상품 상세에서 우대금리 조건을 확인해보세요.",
                    false
            );
        }

        return switch (kind) {
            case EARTH_GUARDIAN -> scoreEarthGuardian(product, feature);
            case WARM_COMPANION -> scoreWarmCompanion(product, feature);
            case SMART_FINANCE -> scoreSmartFinance(product, feature);
            case GREEN_STEP_UP -> scoreGreenStepUp(product, feature);
            case ESG_MASTER -> scoreEsgMaster(product, feature);
        };
    }

    private ScoredProduct scoreEarthGuardian(FinancialProduct product, FinanceRecommendFeature feature) {
        int matchScore = boundedScore(
                feature.averageERatio() * 40
                        + feature.latestERatio() * 20
                        + feature.targetHitRatio(feature.eTargetHitCount()) * 25
                        + consecutiveRatio(feature.latestConsecutiveE()) * 10
                        + benefitRatio(product) * 5
        );

        String reason = feature.eTargetHitCount() >= 2
                ? String.format("최근 3개월 중 %d개월에서 E 월목표를 달성했어요. E 우대금리 조건을 가장 안정적으로 노릴 수 있어요.", feature.eTargetHitCount())
                : String.format("최근 3개월 E 평균 달성률이 %d%%예요. 환경 활동 패턴이 지구 수호대 적금과 가장 잘 맞아요.", percent(feature.averageERatio()));

        String actionable = feature.latestEScore() >= feature.eMonthlyTarget()
                ? "지금처럼 E 활동을 유지하면 다음 달 우대금리 누적을 기대할 수 있어요."
                : String.format("최근 기준으로 E 점수 %d점만 더 채우면 월 우대 조건에 도달해요.", Math.max(feature.eMonthlyTarget() - feature.latestEScore(), 0));

        return new ScoredProduct(product, matchScore, formatRateLabel(product), reason, actionable, false);
    }

    private ScoredProduct scoreWarmCompanion(FinancialProduct product, FinanceRecommendFeature feature) {
        int matchScore = boundedScore(
                feature.averageSRatio() * 40
                        + feature.latestSRatio() * 20
                        + feature.targetHitRatio(feature.sTargetHitCount()) * 25
                        + consecutiveRatio(feature.latestConsecutiveS()) * 10
                        + benefitRatio(product) * 5
        );

        String reason = feature.sTargetHitCount() >= 2
                ? String.format("최근 3개월 중 %d개월에서 S 월목표를 달성했어요. 사회 활동 우대금리 달성 가능성이 가장 높아요.", feature.sTargetHitCount())
                : String.format("최근 3개월 S 평균 달성률이 %d%%예요. 기부·봉사·가치소비 패턴이 따뜻한 동행 적금과 잘 맞아요.", percent(feature.averageSRatio()));

        String actionable = feature.latestSScore() >= feature.sMonthlyTarget()
                ? "최근처럼 S 활동을 이어가면 월별 우대금리 누적 흐름을 유지할 수 있어요."
                : String.format("최근 기준으로 S 점수 %d점만 더 채우면 월 우대 조건에 도달해요.", Math.max(feature.sMonthlyTarget() - feature.latestSScore(), 0));

        return new ScoredProduct(product, matchScore, formatRateLabel(product), reason, actionable, false);
    }

    private ScoredProduct scoreSmartFinance(FinancialProduct product, FinanceRecommendFeature feature) {
        int matchScore = boundedScore(
                feature.averageGRatio() * 40
                        + feature.latestGRatio() * 20
                        + feature.targetHitRatio(feature.gTargetHitCount()) * 25
                        + consecutiveRatio(feature.latestConsecutiveG()) * 10
                        + benefitRatio(product) * 5
        );

        String reason = feature.gTargetHitCount() >= 2
                ? String.format("최근 3개월 중 %d개월에서 G 월목표를 달성했어요. 금융 퀴즈 기반 우대금리 조건을 가장 쉽게 채울 가능성이 높아요.", feature.gTargetHitCount())
                : String.format("최근 3개월 G 평균 달성률이 %d%%예요. 퀴즈 참여 패턴이 바른 금융 스마트 적금과 잘 맞아요.", percent(feature.averageGRatio()));

        String actionable = feature.latestGScore() >= feature.gMonthlyTarget()
                ? String.format("월 우대 %s에 더해 만기 유지 보너스도 노려볼 수 있어요.", formatBps(SavingsProductKind.SMART_FINANCE_MONTHLY_INCREMENT_BPS))
                : String.format("최근 기준으로 G 점수 %d점만 더 채우면 월 우대 조건에 도달해요.", Math.max(feature.gMonthlyTarget() - feature.latestGScore(), 0));

        return new ScoredProduct(product, matchScore, formatRateLabel(product), reason, actionable, false);
    }

    private ScoredProduct scoreGreenStepUp(FinancialProduct product, FinanceRecommendFeature feature) {
        int matchScore = boundedScore(
                feature.scoreGrowthRatio() * 45
                        + feature.averageTotalActivityRatio() * 25
                        + feature.recentActivityRatio() * 15
                        + feature.activeCategoryRatio() * 10
                        + benefitRatio(product) * 5
        );

        String reason = feature.scoreGrowthLastQuarter() > 0
                ? String.format("최근 3개월 총점이 %d점 상승했어요. 점수 상승형 구조인 그린 스텝업 적금과 가장 잘 맞아요.", feature.scoreGrowthLastQuarter())
                : "최근 3개월 활동을 고르게 쌓고 있어요. 범용형 우대 구조인 그린 스텝업 적금으로 시작하기 좋아요.";

        String actionable = String.format(
                "가입 이후 총점이 %d점 오를 때마다 %s 우대가 쌓이는 구조라 최근 상승 흐름을 그대로 활용하기 좋아요.",
                SavingsProductKind.GREEN_STEP_UP_SCORE_STEP,
                formatBps(100)
        );

        return new ScoredProduct(product, matchScore, formatRateLabel(product), reason, actionable, false);
    }

    private ScoredProduct scoreEsgMaster(FinancialProduct product, FinanceRecommendFeature feature) {
        int matchScore = boundedScore(
                feature.masterBufferRatio() * 35
                        + feature.recentOver900Ratio() * 35
                        + feature.recentActivityRatio() * 15
                        + feature.activeCategoryRatio() * 10
                        + benefitRatio(product) * 5
        );

        String reason = feature.recentSnapshotCount() > 0 && feature.over900SnapshotCount() > 0
                ? String.format("최근 3개월 스냅샷 중 %d개월에서 900점 이상을 유지했어요. ESG 마스터 적금 조건과 가장 가깝습니다.", feature.over900SnapshotCount())
                : String.format("현재 총점이 %d점으로 가입 조건을 충족해요. 고금리 적금 후보로 볼 수 있어요.", feature.currentTotalScore());

        String actionable = feature.currentTotalScore() - SavingsProductKind.ESG_MASTER_MIN_TOTAL_SCORE < 30
                ? "900점 아래로 떨어지지 않도록 최근 활동 흐름을 유지하는 것이 가장 중요해요."
                : "지금 점수 버퍼를 유지하면 만기 우대금리를 안정적으로 노릴 수 있어요.";

        return new ScoredProduct(product, matchScore, formatRateLabel(product), reason, actionable, false);
    }

    private double consecutiveRatio(int consecutive) {
        return Math.min(consecutive, 3) / 3.0;
    }

    private double benefitRatio(FinancialProduct product) {
        BigDecimal addedRate = product.getMaxRate().subtract(product.getBaseRate());
        return addedRate.divide(new BigDecimal("6.00"), 4, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE)
                .doubleValue();
    }

    private int percent(double ratio) {
        return (int) Math.round(ratio * 100);
    }

    private int boundedScore(double raw) {
        return (int) Math.max(0, Math.min(Math.round(raw), 100));
    }

    private String formatRateLabel(FinancialProduct product) {
        return "연 " + product.getMaxRate().stripTrailingZeros().toPlainString() + "%";
    }

    private String formatBps(int basisPoints) {
        return new BigDecimal(basisPoints)
                .divide(new BigDecimal("100"), 2, RoundingMode.DOWN)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }

    @Getter
    @RequiredArgsConstructor
    public static class ScoredProduct {
        private final FinancialProduct product;
        private final int matchScore;
        private final String expectedMaxRate;
        private final String reason;
        private final String actionable;
        private final boolean ineligible;
    }
}
