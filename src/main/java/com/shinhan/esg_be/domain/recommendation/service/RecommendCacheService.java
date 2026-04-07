package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX_ACTIVITY = "activity_recommend:";
    private static final String KEY_PREFIX_LOCK = "recommend_lock:";
    private static final Duration TTL_ACTIVITY = Duration.ofMinutes(30);
    private static final Duration TTL_LOCK = Duration.ofSeconds(10);

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

    public void saveActivityRecommend(Long userId, ActivityRecommendResponse response) {
        String key = KEY_PREFIX_ACTIVITY + userId;
        try {
            redisTemplate.opsForValue().set(key, response, TTL_ACTIVITY);
            log.debug("캐시 저장 - key={} TTL={}m", key, TTL_ACTIVITY.toMinutes());
        } catch (Exception e) {
            log.error("캐시 저장 실패 - key={}", key, e);
        }
    }

    public void evictActivityRecommend(Long userId) {
        String key = KEY_PREFIX_ACTIVITY + userId;
        try {
            redisTemplate.delete(key);
            log.info("캐시 삭제 - key={}", key);
        } catch (Exception e) {
            log.error("캐시 삭제 실패 - key={}", key, e);
        }
    }

    public boolean acquireLock(Long userId) {
        String key = KEY_PREFIX_LOCK + userId;
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "locked", TTL_LOCK);
            boolean result = Boolean.TRUE.equals(acquired);
            log.debug("락 획득 {} - key={}", result ? "성공" : "실패", key);
            return result;
        } catch (Exception e) {
            log.error("락 획득 실패 - key={}", key, e);
            return false;
        }
    }

    public void releaseLock(Long userId) {
        String key = KEY_PREFIX_LOCK + userId;
        try {
            redisTemplate.delete(key);
            log.debug("락 해제 - key={}", key);
        } catch (Exception e) {
            log.error("락 해제 실패 - key={}", key, e);
        }
    }

    public boolean isLocked(Long userId) {
        String key = KEY_PREFIX_LOCK + userId;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("락 확인 실패 - key={}", key, e);
            return false;
        }
    }
}
