package com.shinhan.esg_be.domain.recommendation.controller;

import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse;
import com.shinhan.esg_be.domain.recommendation.service.RecommendationService;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AuthContext authContext;
    
    @GetMapping("/recommend")
    public ResponseEntity<ActivityRecommendResponse> getRecommendations() {
        ActivityRecommendResponse response =
                recommendationService.getRecommendations(authContext.currentUserId());

        return ResponseEntity.ok(response);
    }
}
