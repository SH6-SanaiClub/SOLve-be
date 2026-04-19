package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.environment.repository.UserEnvironmentActivityRepository;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.quiz.repository.UserQuizRepository;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.social.repository.UserEcoProductRepository;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularityService {

    private final UserEnvironmentActivityRepository userActivityRepository;
    private final UserDonationRepository userDonationRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final UserEcoProductRepository userEcoProductRepository;
    private final UserQuizRepository userQuizRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String POPULARITY_KEY = "activity_popularity";
    private static final Duration POPULARITY_TTL = Duration.ofHours(24);

    // ── 인기 추천 1개 반환 (전체 후보 기준, 필터 없음) ──────────────────────────────
    public Optional<ActivityCandidateDto> findPopular(List<ActivityCandidateDto> allCandidates) {
        Map<String, Long> popularityMap = getPopularityMap();

        // 전체 후보에서 인기도 1위 선정 (Top3 제외 없음)
        return allCandidates.stream()
                .max(Comparator.comparingLong(c -> resolvePopularityCount(popularityMap, c)));
    }

    // ── 인기도 맵 조회 (Redis 캐시 우선, MISS 시 DB 집계) ──────────
    @SuppressWarnings("unchecked")
    private Map<String, Long> getPopularityMap() {
        try {
            Object cached = redisTemplate.opsForValue().get(POPULARITY_KEY);
            if (cached instanceof Map<?, ?> map) {
                log.debug("인기도 캐시 HIT");
                return (Map<String, Long>) map;
            }
        } catch (Exception e) {
            log.error("인기도 캐시 조회 실패", e);
        }

        log.debug("인기도 캐시 MISS - DB 집계");
        return aggregateAndCache();
    }

    // ── 배치 또는 캐시 MISS 시 DB에서 집계 ─────────────────────────
    public Map<String, Long> aggregateAndCache() {
        LocalDateTime since7 = LocalDateTime.now().minusDays(7);
        Map<String, Long> popularityMap = new HashMap<>();

        // E 활동 (activity_id별 집계)
        userActivityRepository.countApprovedGroupByActivitySince(since7)
                .forEach(row -> popularityMap.put("PHOTO:" + row.getActivityId(), row.getCount()));

        // S 기부 (donation_id별 집계)
        userDonationRepository.countGroupByDonationSince(since7)
                .forEach(row -> popularityMap.put("DONATION:" + row.getDonationId(), row.getCount()));

        // S 봉사 (신청 기준 volunteer_id별 집계)
        userVolunteerRepository.countApplicationsGroupByVolunteerSince(since7)
                .forEach(row -> popularityMap.put("VOLUNTEER:" + row.getVolunteerId(), row.getCount()));

        // S 상품구매 (product_id별 집계)
        userEcoProductRepository.countGroupByProductSince(since7)
                .forEach(row -> popularityMap.put("PURCHASE:" + row.getProductId(), row.getCount()));

        // G 퀴즈 (단일 타입이므로 전체 참여 수)
        long quizCount = userQuizRepository.countByCreatedAtAfter(since7);
        popularityMap.put("QUIZ:0", quizCount);

        // Redis 저장
        try {
            redisTemplate.opsForValue().set(POPULARITY_KEY, popularityMap, POPULARITY_TTL);
            log.info("인기도 캐시 저장 완료 - 활동 수={}", popularityMap.size());
        } catch (Exception e) {
            log.error("인기도 캐시 저장 실패", e);
        }

        return popularityMap;
    }

    public void evictPopularityCache() {
        try {
            redisTemplate.delete(POPULARITY_KEY);
            log.info("인기도 캐시 삭제 완료");
        } catch (Exception e) {
            log.error("인기도 캐시 삭제 실패", e);
        }
    }

    private long resolvePopularityCount(Map<String, Long> popularityMap, ActivityCandidateDto candidate) {
        if ("QUIZ".equals(candidate.getActivityType())) {
            return popularityMap.getOrDefault("QUIZ:0", 0L);
        }
        String key = candidate.getActivityType() + ":" + candidate.getReferenceId();
        return popularityMap.getOrDefault(key, 0L);
    }
}
