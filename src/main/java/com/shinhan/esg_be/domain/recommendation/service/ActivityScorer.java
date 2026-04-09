package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.environment.repository.UserEnvironmentActivityRepository;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.recommendation.entity.AhpWeightPolicy;
import com.shinhan.esg_be.domain.recommendation.repository.AhpWeightPolicyRepository;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.social.repository.UserEcoProductRepository;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityScorer {

    private final AhpWeightPolicyRepository ahpWeightPolicyRepository;
    private final UserEnvironmentActivityRepository userActivityRepository;
    private final UserDonationRepository userDonationRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final UserEcoProductRepository userEcoProductRepository;

    // 월 한도 (정규화 분모)
    private static final Map<String, Integer> MONTHLY_MAX =
            Map.of("E", 5, "S", 25, "G", 10);

    // user_type × 카테고리 Prior 매핑 (AHP 쌍대비교 도출)
    private static final Map<UserType, Map<String, Double>> TYPE_PRIOR = Map.of(
            UserType.GREEN, Map.of("E", 1.0, "S", 0.4, "G", 0.2),
            UserType.SOCIAL, Map.of("E", 0.4, "S", 1.0, "G", 0.2),
            UserType.FINANCE, Map.of("E", 0.2, "S", 0.4, "G", 1.0),
            UserType.ALL_ROUNDER, Map.of("E", 0.7, "S", 0.7, "G", 0.7)
    );

    public List<ActivityCandidateDto> score(
            List<ActivityCandidateDto> candidates,
            UserFeatureDto feature,
            double maxB2Raw
    ) {
        // AHP 가중치 로드
        Map<String, Double> weights = loadWeights();

        double wB = weights.getOrDefault("B_AXIS", 0.55);
        double wC = weights.getOrDefault("C_AXIS", 0.45);
        double wB1 = weights.getOrDefault("B1_GRADE_CONTRIBUTION", 0.45);
        double wB2 = weights.getOrDefault("B2_POINT_EFFICIENCY", 0.30);
        double wB3 = weights.getOrDefault("B3_FINANCE_LINK", 0.25);
        double wC1 = weights.getOrDefault("C1_BEHAVIOR_FIT", 0.70);
        double wC2 = weights.getOrDefault("C2_BALANCE", 0.30);

        // B2 정규화 구간 계산 (후보 전체 기준)
        double maxRaw = candidates.stream()
                .mapToDouble(this::calcB2Raw)
                .max()
                .orElse(0.0);
        if (maxB2Raw > 0) {
            // 외부에서 계산한 maxB2Raw와 내부 계산값 중 더 큰 값 사용
            maxRaw = Math.max(maxRaw, maxB2Raw);
        }
        double minRaw = candidates.stream()
                .mapToDouble(this::calcB2Raw)
                .min()
                .orElse(0.0);
        double range = maxRaw - minRaw;
        if (range == 0.0) {
            range = 1.0;
        }

        LocalDateTime since14 = LocalDateTime.now().minusDays(14);
        for (ActivityCandidateDto c : candidates) {
            // 카테고리 월한도 기준 점수 정규화
            int monthlyMax = MONTHLY_MAX.getOrDefault(c.getScoreCategory(), 1);
            double normalizedScore = clamp01((double) c.getScoreValue() / monthlyMax);
            c.setNormalizedScore(normalizedScore);

            // B1: 등급기여도
            double b1 = calcB1(normalizedScore, feature);
            // B2: 포인트효율
            double b2Normalized = normalizeB2(c, minRaw, range);
            // B3: 금융연계도
            double b3 = calcB3(c, feature);
            double bAxis = b1 * wB1 + b2Normalized * wB2 + b3 * wB3;

            // C1: 행동패턴 적합도
            double c1 = calcC1(c, feature);
            // C2: 카테고리 비율 기반 균형보정
            double c2Ratio = calcCategoryRatio(c, feature);
            double c2 = c2Ratio * 0.30 + 0.70;
            double cAxis = c1 * wC1 + c2 * wC2;

            // ── 랭킹점수 ──
            double rankingScore = bAxis * wB + cAxis * wC;
            // ── 피로도 감점 ──
            double fatigueFactor = calcFatigueFactor(c, feature.getUserId(), since14);
            double finalScore = rankingScore * fatigueFactor;

            c.setBoostValue(0.0);
            c.setFinalScore(finalScore);
            // UI/LLM 노출용 추천 사유 코드
            c.setMainReason(resolveMainReason(c, feature, b1, b2Normalized, c2Ratio));

            log.debug("score type={} refId={} b1={} b2={} b3={} c1={} c2={} final={}",
                    c.getActivityType(), c.getReferenceId(), b1, b2Normalized, b3, c1, c2, finalScore);
        }

        return candidates;
    }

    // ── B1: 등급기여도 (단기 0.6 + 장기 0.4) ──
    private double calcB1(double normalizedScore, UserFeatureDto feature) {
        if (feature.getNextGradeGap() <= 0) {
            // 이미 최고 등급(EARTH) → 등급기여도 최소
            return normalizedScore * 0.4;
        }

        int rangeSize = getGradeRangeSize(feature.getCurrentGrade());
        double ratio = Math.min(Math.max((double) feature.getNextGradeGap() / rangeSize, 0.05), 1.0);
        double shortTerm = Math.min(normalizedScore / ratio, 1.0);
        double longTerm = normalizedScore;
        return shortTerm * 0.6 + longTerm * 0.4;
    }

    // ── B2: 포인트효율 raw 값 ──
    private double calcB2Raw(ActivityCandidateDto c) {
        double rawPoint;
        if (c.getPointValue() > 0) {
            rawPoint = c.getPointValue();
        } else if (c.getPointRate() > 0) {
            // 결제금액 기반 포인트 — 최소 결제금액 기준으로 추정
            // 기부: 3만원 기준, 상품구매: 평균 1만원 기준
            double baseAmount = "DONATION".equals(c.getActivityType()) ? 30000.0 : 10000.0;
            rawPoint = baseAmount * c.getPointRate();
        } else {
            rawPoint = 0.0;
        }
        // raw 효율 = 포인트 / 난이도지수
        return rawPoint / Math.max(c.getDifficultyIndex(), 1.0);
    }

    // ── B2 min-max 정규화 ──
    private double normalizeB2(ActivityCandidateDto candidate, double minRaw, double range) {
        double raw = calcB2Raw(candidate);
        return clamp01((raw - minRaw) / range);
    }

    // ── B3: 금융연계도 ──
    private double calcB3(ActivityCandidateDto c, UserFeatureDto feature) {
        if (!feature.isHasSaving() && !feature.isHasLoan()) {
            return 0.0;
        }

        // 시급성: 적금 만기까지 남은 일수
        double urgency = 0.0;
        if (feature.isHasSaving() && feature.getSavingMaturityDate() != null) {
            long daysToMaturity = ChronoUnit.DAYS.between(LocalDate.now(), feature.getSavingMaturityDate());
            if (daysToMaturity <= 30) {
                urgency = 1.0;
            } else if (daysToMaturity <= 90) {
                urgency = 0.5;
            }
        }
        if (urgency == 0.0) {
            return 0.0;
        }

        // 예상혜택크기: 카테고리 연계 여부 (단순화 — 실제 상품 연계는 FinanceRecommendService에서 정밀 계산)
        double benefit = switch (c.getScoreCategory()) {
            case "E" -> 0.7; // 지구수호대 적금 연계
            case "S" -> 0.8; // 따뜻한동행 적금 연계
            case "G" -> 0.6; // 바른금융스마트 적금 연계
            default -> 0.3;
        };
        return benefit * urgency;
    }

    // ── C1: 행동패턴 적합도 (Prior → 행동 기반 점진 전환) ──
    private double calcC1(ActivityCandidateDto c, UserFeatureDto feature) {
        int totalCount = feature.getRecentECount() + feature.getRecentSCount() + feature.getRecentGCount();

        // α = min(총 활동 횟수 / 30, 0.8)
        double alpha = Math.min(totalCount / 30.0, 0.8);

        // Prior (user_type 기반)
        double prior = TYPE_PRIOR
                .getOrDefault(feature.getUserType(), TYPE_PRIOR.get(UserType.ALL_ROUNDER))
                .getOrDefault(c.getScoreCategory(), 0.5);

        // 행동기반비율
        double behaviorRatio = 0.0;
        if (totalCount > 0) {
            int categoryCount = switch (c.getScoreCategory()) {
                case "E" -> feature.getRecentECount();
                case "S" -> feature.getRecentSCount();
                case "G" -> feature.getRecentGCount();
                default -> 0;
            };
            behaviorRatio = (double) categoryCount / totalCount;
        }

        return clamp01(prior * (1 - alpha) + behaviorRatio * alpha);
    }

    // 해당 후보의 카테고리가 최근 행동에서 차지하는 비율
    private double calcCategoryRatio(ActivityCandidateDto c, UserFeatureDto feature) {
        int total = feature.getRecentECount()
                + feature.getRecentSCount()
                + feature.getRecentGCount();
        if (total == 0) {
            return 1.0 / 3.0;
        }
        int count = switch (c.getScoreCategory()) {
            case "E" -> feature.getRecentECount();
            case "S" -> feature.getRecentSCount();
            case "G" -> feature.getRecentGCount();
            default -> 0;
        };
        return (double) count / total;
    }

    // 점수·상태 기반 대표 추천 사유 1개 선택
    private String resolveMainReason(
            ActivityCandidateDto c,
            UserFeatureDto feature,
            double b1,
            double b2Normalized,
            double c2Ratio
    ) {
        if (c.getDeadlineDate() != null &&
                ChronoUnit.DAYS.between(LocalDate.now(), c.getDeadlineDate()) <= 7) {
            return "마감임박";
        }
        if (b1 >= 0.65) {
            return "다음등급근접";
        }
        if (c2Ratio < 0.2) {
            return "카테고리부족";
        }
        if ("QUIZ".equals(c.getActivityType()) && !feature.isTodayQuizDone()) {
            return "오늘퀴즈미완료";
        }
        if (b2Normalized >= 0.7) {
            return "포인트효율높음";
        }
        if (feature.isInactivityRisk()) {
            return "미활동위험";
        }
        return "종합추천";
    }

    // ── 피로도 감점 (최근 14일 동일 활동 3회 이상 → 0.85) ──
    private double calcFatigueFactor(ActivityCandidateDto c, Long userId, LocalDateTime since14) {
        long count = switch (c.getActivityType()) {
            case "PHOTO" -> userActivityRepository.countRecentByActivity(userId, c.getReferenceId(), since14);
            case "DONATION" -> userDonationRepository.countByUser_UserIdAndDonation_DonationIdAndCreatedAtAfter(
                    userId, c.getReferenceId(), since14);
            case "VOLUNTEER" -> userVolunteerRepository.countByUser_UserIdAndVolunteer_VolunteerIdAndCreatedAtAfter(
                    userId, c.getReferenceId(), since14);
            case "PURCHASE" -> userEcoProductRepository.countByUser_UserIdAndEcoProduct_ProductIdAndCreatedAtAfter(
                    userId, c.getReferenceId(), since14);
            default -> 0L; // QUIZ는 하루 1회라 피로도 불필요
        };
        return count >= 3 ? 0.85 : 1.0;
    }

    private int getGradeRangeSize(Grade grade) {
        return switch (grade) {
            case SEED -> 600;
            case SPROUT -> 100;
            case TREE -> 100;
            case FOREST -> 100;
            case EARTH -> 100;
        };
    }

    // ── AHP 가중치 로드 ──
    private Map<String, Double> loadWeights() {
        return ahpWeightPolicyRepository.findAllByIsActiveTrue()
                .stream()
                .collect(Collectors.toMap(
                        AhpWeightPolicy::getCriterionCode,
                        AhpWeightPolicy::getWeight,
                        (a, b) -> a
                ));
    }

    private double clamp01(double value) {
        if (value < 0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }
}
