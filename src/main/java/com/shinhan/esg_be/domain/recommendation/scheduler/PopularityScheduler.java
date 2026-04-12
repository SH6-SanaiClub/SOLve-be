package com.shinhan.esg_be.domain.recommendation.scheduler;

import com.shinhan.esg_be.domain.recommendation.service.PopularityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularityScheduler {

    private final PopularityService popularityService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(cron = "0 0 * * * *")
    public void aggregatePopularity() {
        log.info("인기도 집계 배치 시작");
        popularityService.aggregateAndCache();
        log.info("인기도 집계 배치 완료");
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void evictAllRecommendCachesOnMonthReset() {
        log.info("월 초기화 - 전체 추천 캐시 삭제 시작");
        Set<String> keys = redisTemplate.keys("activity_recommend:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("월 초기화 - {}개 추천 캐시 삭제 완료", keys.size());
        }
        popularityService.evictPopularityCache();
        log.info("월 초기화 - 완료");
    }
}
