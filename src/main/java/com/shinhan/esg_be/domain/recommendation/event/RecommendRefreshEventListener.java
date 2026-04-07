package com.shinhan.esg_be.domain.recommendation.event;

import com.shinhan.esg_be.domain.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendRefreshEventListener {

    private final RecommendationService recommendationService;

    @Async("recommendAsyncExecutor")
    @EventListener
    public void onRecommendRefresh(RecommendRefreshEvent event) {
        log.info("비동기 추천 갱신 시작 - userId={}", event.userId());
        recommendationService.refreshCache(event.userId());
    }
}
