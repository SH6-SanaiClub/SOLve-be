package com.shinhan.esg_be.domain.recommendation.scheduler;

import com.shinhan.esg_be.domain.recommendation.service.PopularityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularityScheduler {

    private final PopularityService popularityService;

    @Scheduled(cron = "0 0 2 * * *")
    public void aggregatePopularity() {
        log.info("인기도 집계 배치 시작");
        popularityService.aggregateAndCache();
        log.info("인기도 집계 배치 완료");
    }
}
