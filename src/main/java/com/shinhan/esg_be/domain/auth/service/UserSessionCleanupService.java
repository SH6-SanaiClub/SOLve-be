package com.shinhan.esg_be.domain.auth.service;

import com.shinhan.esg_be.domain.ai.service.ChatContextService;
import com.shinhan.esg_be.domain.ai.service.ChatHistoryService;
import com.shinhan.esg_be.domain.recommendation.service.RecommendCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionCleanupService {

    private final RecommendCacheService recommendCacheService;
    private final ChatContextService chatContextService;
    private final ChatHistoryService chatHistoryService;

    public void clearUserCaches(Long userId) {
        recommendCacheService.evictActivityRecommend(userId);
        chatContextService.evict(userId);
        chatHistoryService.clearHistory(userId);
        log.info("로그아웃 사용자 캐시 정리 완료 userId={}", userId);
    }
}
