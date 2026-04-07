package com.shinhan.esg_be.domain.recommendation.controller;

import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse;
import com.shinhan.esg_be.domain.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * ESG 활동 추천 조회
     * GET /api/v1/chat/recommend
     *
     * TODO: JWT 인증 연동 후 @RequestParam Long userId 제거하고
     *       SecurityContextHolder에서 userId 추출로 교체
     *       예시:
     *       Long userId = ((CustomUserDetails) SecurityContextHolder
     *           .getContext().getAuthentication().getPrincipal()).getUserId();
     */
    @GetMapping("/recommend")
    public ResponseEntity<ActivityRecommendResponse> getRecommendations(
            @RequestParam Long userId
    ) {

        ActivityRecommendResponse response =
                recommendationService.getRecommendations(userId);

        return ResponseEntity.ok(response);
    }
}
