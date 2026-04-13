package com.shinhan.esg_be.domain.onboarding.controller;

import com.shinhan.esg_be.domain.onboarding.dto.SurveyStatusResponse;
import com.shinhan.esg_be.domain.onboarding.dto.SurveySubmitRequest;
import com.shinhan.esg_be.domain.onboarding.service.OnboardingService;
import com.shinhan.esg_be.global.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final AuthContext authContext;

    // 설문 완료 + userType 저장
    @PostMapping("/survey")
    public ResponseEntity<Void> submitSurvey(
            @RequestBody @Valid SurveySubmitRequest request
    ) {
        Long userId = authContext.currentUserId();
        onboardingService.submitSurvey(userId, request);
        return ResponseEntity.ok().build();
    }

    // 설문 완료 여부 조회 (프론트 가드용)
    @GetMapping("/status")
    public ResponseEntity<SurveyStatusResponse> getStatus() {
        Long userId = authContext.currentUserId();
        return ResponseEntity.ok(onboardingService.getStatus(userId));
    }
}
