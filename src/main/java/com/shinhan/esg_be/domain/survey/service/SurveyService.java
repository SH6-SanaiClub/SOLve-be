package com.shinhan.esg_be.domain.survey.service;

import com.shinhan.esg_be.domain.recommendation.service.RecommendCacheService;
import com.shinhan.esg_be.domain.survey.dto.SurveyStatusResponse;
import com.shinhan.esg_be.domain.survey.dto.SurveySubmitRequest;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final UserRepository userRepository;
    private final RecommendCacheService recommendCacheService;

    @Transactional
    public void submitSurvey(Long userId, SurveySubmitRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (Boolean.TRUE.equals(user.getIsSurveyCompleted())) {
            log.warn("이미 설문 완료된 사용자 - userId={}", userId);
            return;
        }

        user.completeSurvey(request.getUserType());
        recommendCacheService.evictActivityRecommend(userId);
        log.info("설문 완료 - userId={}, userType={}", userId, request.getUserType());
    }

    @Transactional(readOnly = true)
    public SurveyStatusResponse getStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return new SurveyStatusResponse(
                Boolean.TRUE.equals(user.getIsSurveyCompleted()),
                user.getUserType().name()
        );
    }
}
