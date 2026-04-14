package com.shinhan.esg_be.domain.survey.controller;

import com.shinhan.esg_be.domain.survey.dto.SurveyStatusResponse;
import com.shinhan.esg_be.domain.survey.dto.SurveySubmitRequest;
import com.shinhan.esg_be.domain.survey.service.SurveyService;
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
@RequestMapping("/api/v1/survey")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;
    private final AuthContext authContext;

    // 설문 완료 + userType 저장
    @PostMapping("/submit")
    public ResponseEntity<Void> submitSurvey(
            @RequestBody @Valid SurveySubmitRequest request
    ) {
        Long userId = authContext.currentUserId();
        surveyService.submitSurvey(userId, request);
        return ResponseEntity.ok().build();
    }

    // 설문 완료 여부 조회 (프론트 가드용)
    @GetMapping("/status")
    public ResponseEntity<SurveyStatusResponse> getStatus() {
        Long userId = authContext.currentUserId();
        return ResponseEntity.ok(surveyService.getStatus(userId));
    }
}
