package com.shinhan.esg_be.domain.recommendation.scheduler;

import com.shinhan.esg_be.domain.recommendation.service.PopularityService;
import com.shinhan.esg_be.domain.recommendation.service.RecommendCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularityScheduler {

    private final PopularityService popularityService;
    private final RecommendCacheService recommendCacheService;

    @Scheduled(cron = "0 0 2 * * *")
    public void aggregatePopularity() {
        log.info("인기도 집계 배치 시작");
        popularityService.aggregateAndCache();
        log.info("인기도 집계 배치 완료");
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void evictAllRecommendCachesOnMonthReset() {
        log.info("월 초기화 - 전체 추천 캐시 삭제 시작");
        int deleted = recommendCacheService.evictAllActivityRecommend();
        log.info("월 초기화 - {}개 추천 캐시 삭제 완료", deleted);
        popularityService.evictPopularityCache();
    }
}
