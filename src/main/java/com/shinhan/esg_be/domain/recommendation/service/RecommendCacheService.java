package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX_ACTIVITY = "activity_recommend:";
    private static final Duration TTL_ACTIVITY = Duration.ofMinutes(30);

    // 활동 추천 결과 조회
    public Optional<ActivityRecommendResponse> findActivityRecommend(Long userId) {
        String key = KEY_PREFIX_ACTIVITY + userId;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof ActivityRecommendResponse response) {
                log.debug("캐시 HIT - key={}", key);
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.error("캐시 조회 실패 - key={}", key, e);
        }
        log.debug("캐시 MISS - key={}", key);
        return Optional.empty();
    }

    // 활동 추천 결과 저장
    public void saveActivityRecommend(Long userId, ActivityRecommendResponse response) {
        String key = KEY_PREFIX_ACTIVITY + userId;
        try {
            redisTemplate.opsForValue().set(key, response, TTL_ACTIVITY);
            log.debug("캐시 저장 - key={} TTL={}m", key, TTL_ACTIVITY.toMinutes());
        } catch (Exception e) {
            log.error("캐시 저장 실패 - key={}", key, e);
        }
    }

    // 활동 추천 캐시 삭제 (활동 완료 시 무효화)
    public void evictActivityRecommend(Long userId) {
        String key = KEY_PREFIX_ACTIVITY + userId;
        try {
            redisTemplate.delete(key);
            log.info("캐시 삭제 - key={}", key);
        } catch (Exception e) {
            log.error("캐시 삭제 실패 - key={}", key, e);
        }
    }

    public int evictAllActivityRecommend() {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX_ACTIVITY + "*");
            if (keys == null || keys.isEmpty()) {
                return 0;
            }
            redisTemplate.delete(keys);
            log.info("전체 추천 캐시 삭제 완료 - count={}", keys.size());
            return keys.size();
        } catch (Exception e) {
            log.error("전체 추천 캐시 삭제 실패", e);
            return 0;
        }
    }
}
