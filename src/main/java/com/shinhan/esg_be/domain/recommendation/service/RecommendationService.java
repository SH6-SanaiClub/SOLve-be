package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.ai.service.AIService;
import com.shinhan.esg_be.domain.environment.repository.UserEnvironmentActivityRepository;
import com.shinhan.esg_be.domain.quiz.repository.UserQuizRepository;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse.RecommendedActivity;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final FeatureExtractor          featureExtractor;
    private final ActivityCandidateLoader   candidateLoader;
    private final ActivityFilterService     filterService;
    private final ActivityScorer            scorer;
    private final ActivityBoostService      boostService;
    private final PopularityService         popularityService;
    private final RecommendCacheService     cacheService;
    private final UserEnvironmentActivityRepository userActivityRepository;
    private final UserQuizRepository userQuizRepository;
    private final AIService                 aiService;

    private static final int TOP_N = 3;

    public ActivityRecommendResponse getRecommendations(Long userId) {
        Optional<ActivityRecommendResponse> cached =
                cacheService.findActivityRecommend(userId);
        if (cached.isPresent()) {
            log.info("캐시 HIT - userId={}", userId);
            return cached.get();
        }
        log.info("캐시 MISS - 추천 파이프라인 실행 userId={}", userId);
        return runPipelineAndCache(userId);
    }

    public void evictRecommendCache(Long userId) {
        cacheService.evictActivityRecommend(userId);
        log.info("추천 캐시 삭제 - userId={}", userId);
    }

    private ActivityRecommendResponse runPipelineAndCache(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // Step 1: Feature 추출
        UserFeatureDto feature = featureExtractor.extract(userId);

        // Step 2: 후보 활동 로드
        List<ActivityCandidateDto> candidates = candidateLoader.loadAll(now);

        // Step 3: 하드 필터
        List<ActivityCandidateDto> filtered = filterService.filter(candidates, feature);

        // Step 4: B+C 랭킹 점수 계산
        //   (B2 정규화를 위해 후보 기준 maxB2Raw를 미리 계산)
        double maxB2Raw = filtered.stream()
                .mapToDouble(this::calcB2Raw)
                .max()
                .orElse(1.0);
        filtered = scorer.score(filtered, feature, maxB2Raw);

        // Step 5: 소프트 부스트
        filtered = boostService.applyBoost(filtered, feature);

        // Step 6: 정렬 → Top 3 선정
        List<ActivityCandidateDto> top3 = filtered.stream()
                .sorted(Comparator.comparingDouble(ActivityCandidateDto::getFinalScore).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());

        // Step 7: 인기 추천 선정 (전체 후보 기준)
        Optional<ActivityCandidateDto> popular =
                popularityService.findPopular(candidates);

        // Step 8: RecommendedActivity 변환
        List<RecommendedActivity> activities = top3.stream()
                .map(this::toRecommendedActivity)
                .collect(Collectors.toList());
        RecommendedActivity popularActivity =
                popular.map(c -> toPopularRecommendedActivity(c, feature)).orElse(null);

        // Step 9: LLM 호출 → description + llmSummary 채우기
        AIService.LLMResult llmResult =
                aiService.generateDescriptions(activities, feature);

        // Step 10: 최종 Response 조립 후 Redis 저장 (LLM 완료 후 저장)
        ActivityRecommendResponse response = ActivityRecommendResponse.builder()
                .activities(llmResult.activities())
                .popularActivity(popularActivity)
                .llmSummary(llmResult.summary())
                .build();

        cacheService.saveActivityRecommend(userId, response);
        return response;
    }

    private RecommendedActivity toRecommendedActivity(ActivityCandidateDto c) {
        return RecommendedActivity.builder()
                .activityType(c.getActivityType())
                .referenceId(c.getReferenceId())
                .name(c.getName())
                .scoreCategory(c.getScoreCategory())
                .scoreValue(c.getScoreValue())
                .pointValue(c.getPointValue())
                .pointRate(c.getPointRate())
                .deadlineDate(c.getDeadlineDate())
                .finalScore(c.getFinalScore())
                .mainReason(c.getMainReason())
                .currentAmount(c.getCurrentAmount())
                .targetAmount(c.getTargetAmount())
                .currentEnrolled(c.getCurrentEnrolled())
                .capacity(c.getCapacity())
                .description(null)
                .build();
    }

    // ── 인기 활동 전용 변환 (상태 필드 추가 계산) ────────────────────────────────
    private RecommendedActivity toPopularRecommendedActivity(
            ActivityCandidateDto c, UserFeatureDto feature
    ) {
        boolean alreadyToday = checkAlreadyParticipatedToday(c, feature.getUserId());
        boolean monthlyLimit = checkMonthlyLimitReached(c, feature);

        return RecommendedActivity.builder()
                .activityType(c.getActivityType())
                .referenceId(c.getReferenceId())
                .name(c.getName())
                .scoreCategory(c.getScoreCategory())
                .scoreValue(c.getScoreValue())
                .pointValue(c.getPointValue())
                .pointRate(c.getPointRate())
                .deadlineDate(c.getDeadlineDate())
                .finalScore(c.getFinalScore())
                .mainReason(c.getMainReason())
                .currentAmount(c.getCurrentAmount())
                .targetAmount(c.getTargetAmount())
                .currentEnrolled(c.getCurrentEnrolled())
                .capacity(c.getCapacity())
                .description(null)
                .alreadyParticipatedToday(alreadyToday)
                .monthlyLimitReached(monthlyLimit)
                .build();
    }

    private boolean checkAlreadyParticipatedToday(ActivityCandidateDto c, Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return switch (c.getActivityType()) {
            case "PHOTO" -> userActivityRepository
                    .countTodayApproved(userId, c.getReferenceId(), startOfDay) > 0;
            case "QUIZ" -> userQuizRepository.countToday(userId, startOfDay) > 0;
            default -> false; // S 활동은 일일 참여 제한 없음
        };
    }

    private boolean checkMonthlyLimitReached(ActivityCandidateDto c, UserFeatureDto feature) {
        return switch (c.getScoreCategory()) {
            case "E" -> feature.getMonthlyEScore() >= 5;
            case "S" -> feature.getMonthlySScore() >= 25;
            case "G" -> feature.getMonthlyGScore() >= 10;
            default -> false;
        };
    }

    // B2 raw 효율 계산 (ActivityScorer와 동일 로직 — maxB2 계산용)
    private double calcB2Raw(ActivityCandidateDto c) {
        if (c.getPointValue() > 0) {
            return c.getPointValue();
        }
        if (c.getPointRate() > 0) {
            double base = "DONATION".equals(c.getActivityType()) ? 30000.0 : 10000.0;
            return base * c.getPointRate();
        }
        return 0.0;
    }
}
