package com.shinhan.esg_be.domain.survey.service;

import com.shinhan.esg_be.domain.recommendation.service.RecommendCacheService;
import com.shinhan.esg_be.domain.survey.dto.SurveyStatusResponse;
import com.shinhan.esg_be.domain.survey.dto.SurveySubmitRequest;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
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

        UserType resolvedUserType = resolveUserType(userId, request);
        user.completeSurvey(resolvedUserType);
        recommendCacheService.evictActivityRecommend(userId);
        log.info("설문 완료 - userId={}, userType={}", userId, resolvedUserType);
    }

    @Transactional(readOnly = true)
    public SurveyStatusResponse getStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        boolean surveyCompleted = Boolean.TRUE.equals(user.getIsSurveyCompleted());
        return new SurveyStatusResponse(
                surveyCompleted,
                surveyCompleted ? user.getUserType().name() : null
        );
    }

    private UserType resolveUserType(Long userId, SurveySubmitRequest request) {
        if (request.hasAllWeights()) {
            UserType resolvedUserType = resolveUserTypeFromWeights(
                    request.getEnvironmentWeight(),
                    request.getSocialWeight(),
                    request.getFinanceWeight()
            );

            if (request.getUserType() != null && request.getUserType() != resolvedUserType) {
                log.warn(
                        "설문 결과와 클라이언트 userType 불일치 - userId={}, clientUserType={}, resolvedUserType={}",
                        userId,
                        request.getUserType(),
                        resolvedUserType
                );
            }

            return resolvedUserType;
        }

        if (request.hasAnyWeights()) {
            log.warn("설문 가중치 일부만 전달됨 - userId={}, fallbackUserType={}", userId, request.getUserType());
        }

        if (request.getUserType() != null) {
            return request.getUserType();
        }

        log.warn("설문 결과를 판정할 값이 없어 ALL_ROUNDER로 처리 - userId={}", userId);
        return UserType.ALL_ROUNDER;
    }

    private UserType resolveUserTypeFromWeights(int environmentWeight, int socialWeight, int financeWeight) {
        int maxWeight = Math.max(environmentWeight, Math.max(socialWeight, financeWeight));
        int maxCount = 0;

        if (environmentWeight == maxWeight) {
            maxCount++;
        }
        if (socialWeight == maxWeight) {
            maxCount++;
        }
        if (financeWeight == maxWeight) {
            maxCount++;
        }

        if (maxCount != 1) {
            return UserType.ALL_ROUNDER;
        }

        if (environmentWeight == maxWeight) {
            return UserType.GREEN;
        }
        if (socialWeight == maxWeight) {
            return UserType.SOCIAL;
        }
        return UserType.FINANCE;
    }
}
