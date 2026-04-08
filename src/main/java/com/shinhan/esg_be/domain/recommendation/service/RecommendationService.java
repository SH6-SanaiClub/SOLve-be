package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse.RecommendedActivity;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.recommendation.event.RecommendRefreshEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final FeatureExtractor featureExtractor;
    private final ActivityCandidateLoader candidateLoader;
    private final ActivityFilterService filterService;
    private final ActivityScorer scorer;
    private final ActivityBoostService boostService;
    private final PopularityService popularityService;
    private final RecommendCacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int TOP_N = 3;

    // ── 추천 결과 조회 (캐시 우선) ──────────────────────────────
    public ActivityRecommendResponse getRecommendations(Long userId) {

        // 1. 캐시 HIT → 즉시 반환
        Optional<ActivityRecommendResponse> cached = cacheService.findActivityRecommend(userId);
        if (cached.isPresent()) {
            log.info("캐시 HIT - userId={}", userId);
            return cached.get();
        }

        // 2. 비동기 갱신 중인지 확인 (락 보유 중이면 빈 결과 반환)
        if (cacheService.isLocked(userId)) {
            log.info("추천 갱신 중 - userId={}", userId);
            return buildEmptyResponse();
        }

        // 3. 캐시 MISS → 파이프라인 실행
        log.info("캐시 MISS - 추천 파이프라인 실행 userId={}", userId);
        return runPipelineAndCache(userId);
    }

    // ── 활동 완료 후 비동기 캐시 갱신 트리거 ──────────────────────
    public void triggerRefresh(Long userId) {
        cacheService.evictActivityRecommend(userId);
        eventPublisher.publishEvent(new RecommendRefreshEvent(userId));
        log.info("추천 갱신 이벤트 발행 - userId={}", userId);
    }

    // ── 비동기 리스너에서 직접 호출하는 갱신 메서드 ────────────────
    public void refreshCache(Long userId) {
        if (!cacheService.acquireLock(userId)) {
            log.debug("이미 갱신 중 - userId={}", userId);
            return;
        }

        try {
            runPipelineAndCache(userId);
            log.info("캐시 갱신 완료 - userId={}", userId);
        } catch (Exception e) {
            log.error("캐시 갱신 실패 - userId={}", userId, e);
        } finally {
            cacheService.releaseLock(userId);
        }
    }

    // ── 추천 파이프라인 ────────────────────────────────────────────
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

        // Step 7: 인기 추천 선정 (Top 3와 겹치면 차순위)
        Optional<ActivityCandidateDto> popular = popularityService.findPopular(filtered, top3);

        // Step 8: Response 조립
        ActivityRecommendResponse response = buildResponse(top3, popular);

        // Step 9: 캐시 저장
        cacheService.saveActivityRecommend(userId, response);
        return response;
    }

    // ── Response 조립 ──────────────────────────────────────────────
    private ActivityRecommendResponse buildResponse(
            List<ActivityCandidateDto> top3,
            Optional<ActivityCandidateDto> popular
    ) {
        List<RecommendedActivity> activities = top3.stream()
                .map(this::toRecommendedActivity)
                .collect(Collectors.toList());

        RecommendedActivity popularActivity = popular
                .map(this::toRecommendedActivity)
                .orElse(null);

        return ActivityRecommendResponse.builder()
                .activities(activities)
                .popularActivity(popularActivity)
                .llmSummary(null)
                .build();
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
                .build();
    }

    private ActivityRecommendResponse buildEmptyResponse() {
        return ActivityRecommendResponse.builder()
                .activities(List.of())
                .popularActivity(null)
                .llmSummary(null)
                .build();
    }

    // B2 raw 효율 계산 (ActivityScorer와 동일 로직 — maxB2 계산용)
    private double calcB2Raw(ActivityCandidateDto c) {
        if (c.getPointValue() > 0) {
            return c.getPointValue();
        }
        if (c.getPointRate() > 0) {
            double base = "DONATION".equals(c.getActivityType()) ? 30000.0 : 10000.0;
            return (base * c.getPointRate()) / Math.max(c.getDifficultyIndex(), 1.0);
        }
        return 0.0;
    }
}
