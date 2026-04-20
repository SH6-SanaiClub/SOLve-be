package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.ai.service.AIService;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse.RecommendedActivity;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ActivityAvailabilityService activityAvailabilityService;
    private final RecommendCacheService     cacheService;
    private final AIService                 aiService;

    private static final int TOP_N = 3;

    public ActivityRecommendResponse getRecommendations(Long userId) {
        try {
            Optional<ActivityRecommendResponse> cached =
                    cacheService.findActivityRecommend(userId);
            if (cached.isPresent()) {
                log.info("캐시 HIT - userId={}", userId);
                return cached.get();
            }
            log.info("캐시 MISS - 추천 파이프라인 실행 userId={}", userId);
            return runPipelineAndCache(userId);
        } catch (RuntimeException e) {
            log.error("추천 조회 실패 userId={}", userId, e);
            throw e;
        }
    }

    public void evictRecommendCache(Long userId) {
        cacheService.evictActivityRecommend(userId);
        log.info("추천 캐시 삭제 - userId={}", userId);
    }

    public ActivityRecommendResponse getRecommendationsByCategory(Long userId, String scoreCategory) {
        LocalDateTime now = LocalDateTime.now();

        UserFeatureDto feature = featureExtractor.extract(userId);
        List<ActivityCandidateDto> candidates = candidateLoader.loadAll(now).stream()
                .filter(candidate -> scoreCategory.equals(candidate.getScoreCategory()))
                .collect(Collectors.toList());

        List<ActivityCandidateDto> filtered = filterService.filter(candidates, feature);
        List<ActivityCandidateDto> rankedPrimary = rankCandidates(filtered, feature);

        List<ActivityCandidateDto> selected = new ArrayList<>(rankedPrimary.stream()
                .limit(TOP_N)
                .collect(Collectors.toList()));

        if (selected.size() < TOP_N) {
            List<ActivityCandidateDto> fallbackCandidates = filterService
                    .filterIgnoringMonthlyLimit(candidates, feature)
                    .stream()
                    .filter(candidate -> activityAvailabilityService.evaluate(candidate, feature).monthlyLimitReached())
                    .filter(candidate -> selected.stream().noneMatch(existing -> isSameActivity(existing, candidate)))
                    .collect(Collectors.toList());

            List<ActivityCandidateDto> rankedFallback = rankCandidates(fallbackCandidates, feature);
            int remaining = TOP_N - selected.size();
            selected.addAll(rankedFallback.stream()
                    .limit(remaining)
                    .collect(Collectors.toList()));
        }

        List<RecommendedActivity> activities = selected.stream()
                .map(candidate -> toRecommendedActivity(candidate, feature))
                .collect(Collectors.toList());

        AIService.LLMResult llmResult = aiService.generateDescriptions(activities, feature);
        return ActivityRecommendResponse.builder()
                .activities(llmResult.activities())
                .popularActivity(null)
                .llmSummary(llmResult.summary())
                .build();
    }

    private ActivityRecommendResponse runPipelineAndCache(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // Step 1: Feature 추출
        UserFeatureDto feature = featureExtractor.extract(userId);
        log.info("추천 feature 추출 완료 userId={} weakestCategory={} nextGradeGap={}",
                userId, feature.getWeakestCategory(), feature.getNextGradeGap());

        // Step 2: 후보 활동 로드
        List<ActivityCandidateDto> candidates = candidateLoader.loadAll(now);
        log.info("추천 후보 로드 완료 userId={} candidates={}", userId, candidates.size());

        // Step 3: 실행 가능한 후보 우선 필터링
        List<ActivityCandidateDto> filtered = filterService.filter(candidates, feature);
        log.info("추천 후보 필터 완료 userId={} filtered={}", userId, filtered.size());

        // Step 4: 우선 추천 랭킹 계산
        List<ActivityCandidateDto> rankedPrimary = rankCandidates(filtered, feature);

        // Step 5: Top 3 선정, 부족하면 월 한도만 걸린 후보로 보충
        List<ActivityCandidateDto> top3 = new ArrayList<>(rankedPrimary.stream()
                .limit(TOP_N)
                .collect(Collectors.toList()));

        int fallbackCount = 0;
        if (top3.size() < TOP_N) {
            List<ActivityCandidateDto> fallbackCandidates = filterService
                    .filterIgnoringMonthlyLimit(candidates, feature)
                    .stream()
                    .filter(c -> activityAvailabilityService.evaluate(c, feature).monthlyLimitReached())
                    .filter(c -> top3.stream().noneMatch(selected -> isSameActivity(selected, c)))
                    .collect(Collectors.toList());

            List<ActivityCandidateDto> rankedFallback = rankCandidates(fallbackCandidates, feature);
            int remaining = TOP_N - top3.size();
            List<ActivityCandidateDto> fallbackTop = rankedFallback.stream()
                    .limit(remaining)
                    .collect(Collectors.toList());
            top3.addAll(fallbackTop);
            fallbackCount = fallbackTop.size();
            log.info("추천 fallback 보충 완료 userId={} fallbackCandidates={} fallbackSelected={}",
                    userId, fallbackCandidates.size(), fallbackCount);
        }
        log.info("추천 TOP{} 선정 완료 userId={} top={} fallback={}",
                TOP_N, userId, top3.size(), fallbackCount);

        // Step 7: 인기 추천 선정 (전체 후보 기준)
        Optional<ActivityCandidateDto> popular =
                popularityService.findPopular(candidates);

        // Step 8: RecommendedActivity 변환
        List<RecommendedActivity> activities = top3.stream()
                .map(c -> toRecommendedActivity(c, feature))
                .collect(Collectors.toList());
        RecommendedActivity popularActivity =
                popular.map(c -> toRecommendedActivity(c, feature)).orElse(null);

        // Step 9: LLM 호출 → description + llmSummary 채우기
        AIService.LLMResult llmResult =
                aiService.generateDescriptions(activities, feature);
        log.info("추천 LLM 처리 완료 userId={} describedActivities={} hasSummary={}",
                userId, llmResult.activities().size(), llmResult.summary() != null);

        // Step 10: 최종 Response 조립 후 Redis 저장 (LLM 완료 후 저장)
        ActivityRecommendResponse response = ActivityRecommendResponse.builder()
                .activities(llmResult.activities())
                .popularActivity(popularActivity)
                .llmSummary(llmResult.summary())
                .build();

        cacheService.saveActivityRecommend(userId, response);
        log.info("추천 응답 생성 및 캐시 저장 완료 userId={} activities={} popular={}",
                userId, response.getActivities().size(), response.getPopularActivity() != null);
        return response;
    }

    private List<ActivityCandidateDto> rankCandidates(
            List<ActivityCandidateDto> candidates,
            UserFeatureDto feature
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        double maxB2Raw = candidates.stream()
                .mapToDouble(this::calcB2Raw)
                .max()
                .orElse(1.0);
        List<ActivityCandidateDto> scored = scorer.score(candidates, feature, maxB2Raw);
        return boostService.applyBoost(scored, feature).stream()
                .sorted(Comparator.comparingDouble(ActivityCandidateDto::getFinalScore).reversed())
                .collect(Collectors.toList());
    }

    private boolean isSameActivity(ActivityCandidateDto left, ActivityCandidateDto right) {
        return left.getActivityType().equals(right.getActivityType())
                && left.getReferenceId().equals(right.getReferenceId());
    }

    private RecommendedActivity toRecommendedActivity(
            ActivityCandidateDto c,
            UserFeatureDto feature
    ) {
        ActivityAvailabilityStatus status = activityAvailabilityService.evaluate(c, feature);

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
                .alreadyParticipatedToday(status.alreadyParticipatedToday())
                .monthlyLimitReached(status.monthlyLimitReached())
                .canParticipate(status.canParticipate())
                .blockedReasonCode(status.blockedReasonCode())
                .build();
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
