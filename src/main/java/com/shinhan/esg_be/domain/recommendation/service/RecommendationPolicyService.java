package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationPolicyService {

    private static final Map<ScoreCategory, Integer> DEFAULT_MONTHLY_MAX = Map.of(
            ScoreCategory.E, 5,
            ScoreCategory.S, 25,
            ScoreCategory.G_ACTIVITY, 10
    );

    private final EsgScorePolicyRepository esgScorePolicyRepository;

    public int getMonthlyMaxScore(String scoreCategory) {
        ScoreCategory category = mapScoreCategory(scoreCategory);
        if (category == null) {
            return 0;
        }

        return getMonthlyMaxScore(category);
    }

    public int getMonthlyMaxScore(ScoreCategory category) {
        Integer defaultValue = DEFAULT_MONTHLY_MAX.getOrDefault(category, 0);
        return esgScorePolicyRepository.findByCategoryAndIsActiveTrue(category)
                .map(EsgScorePolicy::getMonthlyMaxScore)
                .filter(value -> value != null && value > 0)
                .orElse(defaultValue);
    }

    public Map<String, Integer> getMonthlyMaxScoreMap() {
        Map<String, Integer> result = new java.util.HashMap<>();
        result.put("E", getMonthlyMaxScore(ScoreCategory.E));
        result.put("S", getMonthlyMaxScore(ScoreCategory.S));
        result.put("G", getMonthlyMaxScore(ScoreCategory.G_ACTIVITY));
        return result;
    }

    private ScoreCategory mapScoreCategory(String scoreCategory) {
        return switch (scoreCategory) {
            case "E" -> ScoreCategory.E;
            case "S" -> ScoreCategory.S;
            case "G" -> ScoreCategory.G_ACTIVITY;
            default -> null;
        };
    }
}
