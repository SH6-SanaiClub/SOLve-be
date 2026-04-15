package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.response.SavingsRecommendResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceRecommendService {

    private final UserMonthlyStatRepository monthlyStatRepository;
    private final FinancialProductRepository financialProductRepository;
    private final UserSavingRepository userSavingRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;
    private final UserRepository userRepository;

    // DB name 기준
    private static final String GREEN_STEP_UP = "그린 스텝업 적금";
    private static final String EARTH_GUARDIAN = "지구 수호대 적금";
    private static final String WARM_COMPANION = "따뜻한 동행 적금";
    private static final String SMART_FINANCE = "바른 금융 스마트 적금";
    private static final String ESG_MASTER = "ESG 마스터 적금";

    public SavingsRecommendResponse recommend(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        Map<String, FinancialProduct> availableSavingsByName = getAvailableSavingsByName(user.getUserId());
        if (availableSavingsByName.isEmpty()) {
            return buildNoAvailableResponse();
        }

        long activityMonths = monthlyStatRepository.countByUser_UserId(user.getUserId());
        if (activityMonths == 0) {
            return buildNewUserResponse(availableSavingsByName);
        }

        List<UserMonthlyStat> recentStats = monthlyStatRepository.findTop3ByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
        if (recentStats.isEmpty()) {
            return buildNewUserResponse(availableSavingsByName);
        }

        boolean hasPenalty = validScoreHistoryRepository.existsByUserAndReason(user, ScoreReason.ABUSE);
        double multiplier = calcMultiplier(activityMonths);
        UserMonthlyStat current = recentStats.get(0);

        List<ScoredProduct> candidates = new ArrayList<>();
        if (availableSavingsByName.containsKey(EARTH_GUARDIAN)) {
            candidates.add(scoreEarthGuardian(current, multiplier));
        }
        if (availableSavingsByName.containsKey(WARM_COMPANION)) {
            candidates.add(scoreWarmCompanion(current, multiplier));
        }
        if (availableSavingsByName.containsKey(SMART_FINANCE)) {
            candidates.add(scoreSmartFinance(current, hasPenalty, multiplier));
        }
        if (availableSavingsByName.containsKey(GREEN_STEP_UP)) {
            candidates.add(scoreGreenStepUp(recentStats, multiplier));
        }
        if (availableSavingsByName.containsKey(ESG_MASTER)) {
            candidates.add(scoreEsgMaster(user.getTotalScore(), current, hasPenalty, multiplier));
        }

        candidates.sort(Comparator
                .comparing(ScoredProduct::isIneligible)
                .thenComparing(Comparator.comparingInt(ScoredProduct::getMatchScore).reversed()));

        ScoredProduct best = candidates.isEmpty()
                ? buildGenericFallback(availableSavingsByName.values().stream().findFirst().orElseThrow())
                : candidates.get(0);

        return SavingsRecommendResponse.builder()
                .isNewUser(false)
                .recommendation(toResponse(best, availableSavingsByName))
                .build();
    }

    private Map<String, FinancialProduct> getAvailableSavingsByName(Long userId) {
        var activeSavingProductIds = userSavingRepository.findAllByUser_UserIdAndStatus(userId, SavingStatus.ACTIVE)
                .stream()
                .map(userSaving -> userSaving.getFinancialProduct().getFinProductId())
                .collect(Collectors.toSet());

        return financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS)
                .stream()
                .filter(product -> !activeSavingProductIds.contains(product.getFinProductId()))
                .collect(Collectors.toMap(
                        FinancialProduct::getName,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private ScoredProduct scoreEarthGuardian(UserMonthlyStat current, double multiplier) {
        int base = (int) ((current.getMonthlyEScore() / 5.0) * 70);
        int consecutive = calcConsecutiveBonus(current.getConsecutiveEMaxScore());
        int raw = Math.min(base + consecutive, 100);
        int score = (int) (raw * multiplier);

        String reason = buildEarthGuardianReason(current.getMonthlyEScore(), current.getConsecutiveEMaxScore());
        String actionable = buildEarthGuardianActionable(current.getMonthlyEScore());

        return ScoredProduct.of(EARTH_GUARDIAN, score, "연 5.0%", reason, actionable, false);
    }

    private ScoredProduct scoreWarmCompanion(UserMonthlyStat current, double multiplier) {
        int base = (int) ((current.getMonthlySScore() / 25.0) * 70);
        int consecutive = calcConsecutiveBonus(current.getConsecutiveSMaxScore());
        int raw = Math.min(base + consecutive, 100);
        int score = (int) (raw * multiplier);

        String reason = buildWarmCompanionReason(current.getMonthlySScore(), current.getConsecutiveSMaxScore());
        String actionable = buildWarmCompanionActionable(current.getMonthlySScore());

        return ScoredProduct.of(WARM_COMPANION, score, "연 7.0%", reason, actionable, false);
    }

    private ScoredProduct scoreSmartFinance(UserMonthlyStat current, boolean hasPenalty, double multiplier) {
        int base = (int) ((current.getMonthlyGScore() / 10.0) * 50);
        int consecutive = calcConsecutiveBonus(current.getConsecutiveGMaxScore());
        int penaltyBonus = hasPenalty ? -20 : 20;
        int raw = Math.min(Math.max(base + consecutive + penaltyBonus, 0), 100);
        int score = (int) (raw * multiplier);

        String reason = buildSmartFinanceReason(current.getMonthlyGScore(), current.getConsecutiveGMaxScore(), hasPenalty);
        String actionable = buildSmartFinanceActionable(current.getMonthlyGScore(), hasPenalty);

        return ScoredProduct.of(SMART_FINANCE, score, "연 5.5%", reason, actionable, false);
    }

    private ScoredProduct scoreGreenStepUp(List<UserMonthlyStat> stats, double multiplier) {
        int avgMonthlyTotal = calcAvgMonthlyIncrease(stats);
        int base;
        if (avgMonthlyTotal >= 15) {
            base = 70;
        } else if (avgMonthlyTotal >= 10) {
            base = 55;
        } else if (avgMonthlyTotal >= 5) {
            base = 40;
        } else if (avgMonthlyTotal >= 0) {
            base = 25;
        } else {
            base = 10;
        }

        UserMonthlyStat current = stats.get(0);
        int activeCategories = (current.getConsecutiveEMaxScore() >= 1 ? 1 : 0)
                + (current.getConsecutiveSMaxScore() >= 1 ? 1 : 0)
                + (current.getConsecutiveGMaxScore() >= 1 ? 1 : 0);

        int raw = Math.min(base + activeCategories * 10, 100);
        int score = (int) (raw * multiplier);

        String reason = buildGreenStepUpReason(avgMonthlyTotal, activeCategories);
        String actionable = "꾸준히 활동할수록 점수가 오르고, 오를수록 금리도 함께 올라가요!";

        return ScoredProduct.of(GREEN_STEP_UP, score, "연 5.0%", reason, actionable, false);
    }

    private ScoredProduct scoreEsgMaster(int totalScore, UserMonthlyStat current, boolean hasPenalty, double multiplier) {
        if (totalScore < 900) {
            int gap = 900 - totalScore;
            return ScoredProduct.of(
                    ESG_MASTER,
                    0,
                    "연 10.0%",
                    String.format("현재 %d점으로 900점 유지 조건이 필요해요. 약 %d점 더 쌓으면 도전할 수 있어요!", totalScore, gap),
                    "E/S/G 활동을 꾸준히 이어가면 언젠가 최고 금리에 도전할 수 있어요.",
                    true
            );
        }

        int surplus = Math.min((totalScore - 900) / 100 * 20, 30);
        int penaltyBonus = hasPenalty ? 0 : 40;
        int consecutiveAll = (current.getConsecutiveEMaxScore() >= 2 ? 1 : 0)
                + (current.getConsecutiveSMaxScore() >= 2 ? 1 : 0)
                + (current.getConsecutiveGMaxScore() >= 2 ? 1 : 0);
        int consecutiveBonus = consecutiveAll == 3 ? 30 : consecutiveAll == 2 ? 20 : 10;
        int raw = Math.min(surplus + penaltyBonus + consecutiveBonus, 100);
        int score = (int) (raw * multiplier);

        return ScoredProduct.of(
                ESG_MASTER,
                score,
                "연 10.0%",
                String.format("현재 %d점으로 900점 유지 가능성이 높아요. 최고 연 10%% 금리를 노려보세요!", totalScore),
                hasPenalty ? "패널티 없이 활동을 유지하는 것이 중요해요." : "지금처럼 꾸준히만 하면 돼요!",
                false
        );
    }

    private double calcMultiplier(long months) {
        if (months == 1) {
            return 0.7;
        }
        if (months == 2) {
            return 0.85;
        }
        return 1.0;
    }

    private int calcConsecutiveBonus(int consecutive) {
        if (consecutive >= 3) {
            return 30;
        }
        if (consecutive == 2) {
            return 20;
        }
        if (consecutive == 1) {
            return 10;
        }
        return 0;
    }

    private int calcAvgMonthlyIncrease(List<UserMonthlyStat> stats) {
        if (stats.size() < 2) {
            return 0;
        }
        int newest = monthlyTotal(stats.get(0));
        int oldest = monthlyTotal(stats.get(stats.size() - 1));
        return (newest - oldest) / (stats.size() - 1);
    }

    private int monthlyTotal(UserMonthlyStat stat) {
        return stat.getMonthlyEScore() + stat.getMonthlySScore() + stat.getMonthlyGScore();
    }

    private SavingsRecommendResponse buildNewUserResponse(Map<String, FinancialProduct> availableSavingsByName) {
        FinancialProduct recommendedProduct = availableSavingsByName.getOrDefault(
                GREEN_STEP_UP,
                availableSavingsByName.values().stream().findFirst().orElseThrow()
        );

        return SavingsRecommendResponse.builder()
                .isNewUser(true)
                .recommendation(SavingsRecommendResponse.RecommendItem.builder()
                        .productId(recommendedProduct.getFinProductId())
                        .productName(recommendedProduct.getName())
                        .matchScore(0)
                        .expectedMaxRate(formatRateLabel(recommendedProduct))
                        .reason("아직 활동 데이터가 부족해요. 활동을 시작하면 맞춤 추천이 가능해요.")
                        .actionable("가입 가능한 적금으로 꾸준히 플랫폼 활동을 이어나가는 건 어떨까요?")
                        .isNewUserRecommend(true)
                        .isAlreadyJoined(false)
                        .isIneligible(false)
                        .build())
                .build();
    }

    private SavingsRecommendResponse buildNoAvailableResponse() {
        return SavingsRecommendResponse.builder()
                .isNewUser(false)
                .recommendation(null)
                .build();
    }

    private ScoredProduct buildGenericFallback(FinancialProduct product) {
        return ScoredProduct.of(
                product.getName(),
                0,
                formatRateLabel(product),
                "현재 맞춤 규칙과 정확히 일치하는 추천 상품이 없어 가입 가능한 적금 중 하나를 안내해드려요.",
                "현재 가입 가능한 적금 상품을 확인하고 원하는 혜택 구조를 선택해보세요.",
                false
        );
    }

    private SavingsRecommendResponse.RecommendItem toResponse(
            ScoredProduct product,
            Map<String, FinancialProduct> availableSavingsByName
    ) {
        FinancialProduct financialProduct = availableSavingsByName.get(product.getProductName());
        if (financialProduct == null) {
            throw new BadRequestException("추천 대상 적금 상품을 찾을 수 없습니다.");
        }

        return SavingsRecommendResponse.RecommendItem.builder()
                .productId(financialProduct.getFinProductId())
                .productName(product.getProductName())
                .matchScore(product.getMatchScore())
                .expectedMaxRate(product.getExpectedMaxRate())
                .reason(product.getReason())
                .actionable(product.getActionable())
                .isNewUserRecommend(false)
                .isAlreadyJoined(false)
                .isIneligible(product.isIneligible())
                .build();
    }

    private String formatRateLabel(FinancialProduct product) {
        return "연 " + product.getMaxRate().stripTrailingZeros().toPlainString() + "%";
    }

    private String buildEarthGuardianReason(int eScore, int consecutive) {
        int pct = (int) (eScore / 5.0 * 100);
        if (consecutive >= 3) {
            return String.format("E 활동 %d개월 연속 만점 달성! 이번 달도 %d%%를 달성 중이에요.", consecutive, pct);
        }
        if (eScore >= 4) {
            return String.format("이번 달 E 활동 달성률 %d%%로 우대금리 조건에 매우 근접해 있어요.", pct);
        }
        return String.format("이번 달 E 활동 달성률 %d%%예요. 조금만 더 하면 우대금리를 받을 수 있어요!", pct);
    }

    private String buildEarthGuardianActionable(int eScore) {
        int remain = 5 - eScore;
        if (remain <= 0) {
            return "이번 달 E 목표를 이미 달성했어요! 다음 달도 이어가 볼까요?";
        }
        return String.format("텀블러/공유자전거/전기차 인증을 %d회 더 하면 이번 달 우대금리 조건 달성이에요!", remain);
    }

    private String buildWarmCompanionReason(int sScore, int consecutive) {
        int pct = (int) (sScore / 25.0 * 100);
        if (consecutive >= 3) {
            return String.format("S 활동 %d개월 연속 만점! 기부/봉사/구매를 꾸준히 이어가고 있어요.", consecutive);
        }
        if (sScore >= 20) {
            return String.format("이번 달 S 활동 달성률 %d%%로 우대금리 조건에 매우 근접해 있어요.", pct);
        }
        return String.format("이번 달 S 활동 달성률 %d%%예요. 기부나 가치가게 구매로 금리를 높여보세요!", pct);
    }

    private String buildWarmCompanionActionable(int sScore) {
        if (sScore >= 25) {
            return "이번 달 S 목표 달성! 매달 유지하면 우대금리가 계속 쌓여요.";
        }
        return "기부 또는 가치가게 구매를 이번 달 1회 이상 유지하면 +0.5% 우대금리가 적용돼요.";
    }

    private String buildSmartFinanceReason(int gScore, int consecutive, boolean hasPenalty) {
        if (hasPenalty) {
            return "패널티 이력이 있어요. 앞으로 꾸준히 활동하면 조건을 회복할 수 있어요.";
        }
        int pct = (int) (gScore / 10.0 * 100);
        if (consecutive >= 3) {
            return String.format("G 퀴즈 %d개월 연속 만점! 패널티도 없어서 우대금리 조건이 아주 좋아요.", consecutive);
        }
        return String.format("이번 달 G 퀴즈 달성률 %d%%예요. 매일 퀴즈에 참여하면 우대금리를 받을 수 있어요!", pct);
    }

    private String buildSmartFinanceActionable(int gScore, boolean hasPenalty) {
        if (hasPenalty) {
            return "패널티 없이 활동을 이어가는 게 가장 중요해요.";
        }
        int remain = 10 - gScore;
        if (remain <= 0) {
            return "이번 달 G 목표 달성! 만기까지 패널티 없이 유지하면 추가 금리도 받아요.";
        }
        return String.format("퀴즈를 %d회 더 참여하면 이번 달 G 목표 달성이에요!", remain);
    }

    private String buildGreenStepUpReason(int avgIncrease, int activeCategories) {
        if (avgIncrease >= 10) {
            return String.format("최근 월평균 %d점씩 상승 중! 점수가 50점 오를 때마다 금리가 1%% 추가돼요.", avgIncrease);
        }
        if (activeCategories >= 2) {
            return "E/S/G 활동을 고르게 하고 있어요. 꾸준히 이어가면 점수가 빠르게 오를 거예요!";
        }
        return "활동을 시작한 만큼 점수가 오르고, 오를수록 금리도 함께 올라가는 구조예요.";
    }

    @Getter
    @AllArgsConstructor(staticName = "of")
    private static class ScoredProduct {
        private String productName;
        private int matchScore;
        private String expectedMaxRate;
        private String reason;
        private String actionable;
        private boolean ineligible;
    }
}
