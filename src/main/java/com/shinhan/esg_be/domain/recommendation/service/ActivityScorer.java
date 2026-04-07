package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.activity.repository.UserActivityRepository;
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
    private final UserActivityRepository userActivityRepository;
    private final UserDonationRepository userDonationRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final UserEcoProductRepository userEcoProductRepository;

    private static final Map<Grade, int[]> GRADE_RANGE = Map.of(
            Grade.SEED, new int[]{0, 599},
            Grade.SPROUT, new int[]{600, 699},
            Grade.TREE, new int[]{700, 799},
            Grade.FOREST, new int[]{800, 899},
            Grade.EARTH, new int[]{900, 1000}
    );

    private static final Grade[] GRADE_ORDER =
            {Grade.SEED, Grade.SPROUT, Grade.TREE, Grade.FOREST, Grade.EARTH};

    private static final Map<String, Integer> MONTHLY_MAX =
            Map.of("E", 5, "S", 25, "G", 10);

    private static final Map<UserType, Map<String, Double>> TYPE_PRIOR = Map.of(
            UserType.GREEN, Map.of("E", 1.0, "S", 0.4, "G", 0.2),
            UserType.SOCIAL, Map.of("E", 0.4, "S", 1.0, "G", 0.2),
            UserType.FINANCE, Map.of("E", 0.2, "S", 0.4, "G", 1.0),
            UserType.ALL_ROUNDER, Map.of("E", 0.7, "S", 0.7, "G", 0.7)
    );

    public List<ActivityCandidateDto> score(
            List<ActivityCandidateDto> candidates,
            UserFeatureDto feature
    ) {
        Map<String, Double> weights = loadWeights();

        double wB = weights.getOrDefault("B_AXIS", 0.55);
        double wC = weights.getOrDefault("C_AXIS", 0.45);
        double wB1 = weights.getOrDefault("B1_GRADE_CONTRIBUTION", 0.45);
        double wB2 = weights.getOrDefault("B2_POINT_EFFICIENCY", 0.30);
        double wB3 = weights.getOrDefault("B3_FINANCE_LINK", 0.25);
        double wC1 = weights.getOrDefault("C1_BEHAVIOR_FIT", 0.70);
        double wC2 = weights.getOrDefault("C2_BALANCE", 0.30);

        double maxB2Raw = candidates.stream()
                .mapToDouble(this::calcB2Raw)
                .max()
                .orElse(0.0);

        LocalDateTime since14 = LocalDateTime.now().minusDays(14);
        for (ActivityCandidateDto c : candidates) {
            int monthlyMax = MONTHLY_MAX.getOrDefault(c.getScoreCategory(), 1);
            double normalizedScore = clamp01((double) c.getScoreValue() / monthlyMax);
            c.setNormalizedScore(normalizedScore);

            double b1 = calcB1(normalizedScore, feature);
            double b2 = maxB2Raw > 0 ? clamp01(calcB2Raw(c) / maxB2Raw) : 0.0;
            double b3 = calcB3(c, feature);
            double bAxis = b1 * wB1 + b2 * wB2 + b3 * wB3;

            double c1 = calcC1(c, feature);
            double c2 = calcC2(c, feature);
            double cAxis = c1 * wC1 + c2 * wC2;

            double rankingScore = bAxis * wB + cAxis * wC;
            double fatigueFactor = calcFatigueFactor(c, feature.getUserId(), since14);
            double finalScore = rankingScore * fatigueFactor;

            c.setBoostValue(0.0);
            c.setFinalScore(finalScore);

            log.debug("score type={} refId={} b1={} b2={} b3={} c1={} c2={} final={}",
                    c.getActivityType(), c.getReferenceId(), b1, b2, b3, c1, c2, finalScore);
        }

        return candidates;
    }

    private double calcB1(double normalizedScore, UserFeatureDto feature) {
        int totalScore = feature.getEScore()
                + feature.getSScore()
                + feature.getGActivityScore()
                + feature.getGRepaymentScore();

        Grade current = feature.getCurrentGrade();
        Grade next = nextGrade(current);
        if (next == null) {
            return normalizedScore * 0.4;
        }

        int nextMin = GRADE_RANGE.get(next)[0];
        int currentMin = GRADE_RANGE.get(current)[0];
        int currentMax = GRADE_RANGE.get(current)[1];
        int rangeSize = currentMax - currentMin + 1;
        int remaining = Math.max(1, nextMin - totalScore);

        double ratio = Math.min(Math.max((double) remaining / rangeSize, 0.05), 1.0);
        double shortTerm = Math.min(normalizedScore / ratio, 1.0);
        double longTerm = normalizedScore;
        return shortTerm * 0.6 + longTerm * 0.4;
    }

    private double calcB2Raw(ActivityCandidateDto c) {
        double rawPoint;
        if (c.getPointValue() > 0) {
            rawPoint = c.getPointValue();
        } else if (c.getPointRate() > 0) {
            double baseAmount = "DONATION".equals(c.getActivityType()) ? 30000.0 : 10000.0;
            rawPoint = baseAmount * c.getPointRate();
        } else {
            rawPoint = 0.0;
        }
        return rawPoint / Math.max(c.getDifficultyIndex(), 1.0);
    }

    private double calcB3(ActivityCandidateDto c, UserFeatureDto feature) {
        if (!feature.isHasSaving() && !feature.isHasLoan()) {
            return 0.0;
        }

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

        double benefit = switch (c.getScoreCategory()) {
            case "E" -> 0.7;
            case "S" -> 0.8;
            case "G" -> 0.6;
            default -> 0.3;
        };
        return benefit * urgency;
    }

    private double calcC1(ActivityCandidateDto c, UserFeatureDto feature) {
        int totalCount = feature.getRecentECount() + feature.getRecentSCount() + feature.getRecentGCount();
        double alpha = Math.min(totalCount / 30.0, 0.8);

        double prior = TYPE_PRIOR
                .getOrDefault(feature.getUserType(), TYPE_PRIOR.get(UserType.ALL_ROUNDER))
                .getOrDefault(c.getScoreCategory(), 0.5);

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

    private double calcC2(ActivityCandidateDto c, UserFeatureDto feature) {
        int totalCount = feature.getRecentECount() + feature.getRecentSCount() + feature.getRecentGCount();

        double categoryRatio;
        if (totalCount == 0) {
            categoryRatio = 1.0 / 3.0;
        } else {
            int categoryCount = switch (c.getScoreCategory()) {
                case "E" -> feature.getRecentECount();
                case "S" -> feature.getRecentSCount();
                case "G" -> feature.getRecentGCount();
                default -> 0;
            };
            categoryRatio = (double) categoryCount / totalCount;
        }

        double raw = 1.0 - categoryRatio;
        return raw * 0.30 + 0.70;
    }

    private double calcFatigueFactor(ActivityCandidateDto c, Long userId, LocalDateTime since14) {
        long count = switch (c.getActivityType()) {
            case "PHOTO" -> userActivityRepository.countRecentByActivity(userId, c.getReferenceId(), since14);
            case "DONATION" -> userDonationRepository.countByUser_UserIdAndDonation_DonationIdAndCreatedAtAfter(
                    userId, c.getReferenceId(), since14);
            case "VOLUNTEER" -> userVolunteerRepository.countByUser_UserIdAndVolunteer_VolunteerIdAndCreatedAtAfter(
                    userId, c.getReferenceId(), since14);
            case "PURCHASE" -> userEcoProductRepository.countByUser_UserIdAndEcoProduct_ProductIdAndCreatedAtAfter(
                    userId, c.getReferenceId(), since14);
            default -> 0L;
        };
        return count >= 3 ? 0.85 : 1.0;
    }

    private Grade nextGrade(Grade current) {
        for (int i = 0; i < GRADE_ORDER.length - 1; i++) {
            if (GRADE_ORDER[i] == current) {
                return GRADE_ORDER[i + 1];
            }
        }
        return null;
    }

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
