package com.shinhan.esg_be.domain.survey.service;

import com.shinhan.esg_be.domain.recommendation.service.RecommendCacheService;
import com.shinhan.esg_be.domain.survey.dto.SurveyStatusResponse;
import com.shinhan.esg_be.domain.survey.dto.SurveySubmitRequest;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @InjectMocks
    private SurveyService surveyService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendCacheService recommendCacheService;

    @Test
    @DisplayName("설문 가중치가 동점이면 올라운더로 저장한다")
    void submitSurveyResolvesTieToAllRounder() {
        User user = createUser();
        SurveySubmitRequest request = createRequest(null, 1, 0, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        surveyService.submitSurvey(1L, request);

        assertThat(user.getIsSurveyCompleted()).isTrue();
        assertThat(user.getUserType()).isEqualTo(UserType.ALL_ROUNDER);
        verify(recommendCacheService).evictActivityRecommend(1L);
    }

    @Test
    @DisplayName("설문 가중치가 있으면 클라이언트 userType보다 서버 판정값을 우선한다")
    void submitSurveyUsesResolvedTypeWhenWeightsArePresent() {
        User user = createUser();
        SurveySubmitRequest request = createRequest(UserType.FINANCE, 2, 1, 0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        surveyService.submitSurvey(1L, request);

        assertThat(user.getIsSurveyCompleted()).isTrue();
        assertThat(user.getUserType()).isEqualTo(UserType.GREEN);
    }

    @Test
    @DisplayName("설문 판정값이 비어 있으면 올라운더로 처리하고 완료 상태를 저장한다")
    void submitSurveyFallsBackToAllRounderWhenRequestIsEmpty() {
        User user = createUser();
        SurveySubmitRequest request = createRequest(null, null, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        surveyService.submitSurvey(1L, request);

        assertThat(user.getIsSurveyCompleted()).isTrue();
        assertThat(user.getUserType()).isEqualTo(UserType.ALL_ROUNDER);
        verify(recommendCacheService).evictActivityRecommend(1L);
    }

    @Test
    @DisplayName("설문 미완료 상태 조회 시 userType은 null을 반환한다")
    void getStatusReturnsNullUserTypeWhenSurveyIsIncomplete() {
        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        SurveyStatusResponse response = surveyService.getStatus(1L);

        assertThat(response.isSurveyCompleted()).isFalse();
        assertThat(response.getUserType()).isNull();
    }

    private User createUser() {
        return User.create(
                "survey-user",
                "encoded-password",
                "설문유저",
                "survey-user@test.com",
                "010-1234-5678",
                LocalDate.of(2000, 1, 1),
                "ci-di-survey"
        );
    }

    private SurveySubmitRequest createRequest(
            UserType userType,
            Integer environmentWeight,
            Integer socialWeight,
            Integer financeWeight
    ) {
        SurveySubmitRequest request = newInstance(SurveySubmitRequest.class);
        ReflectionTestUtils.setField(request, "userType", userType);
        ReflectionTestUtils.setField(request, "environmentWeight", environmentWeight);
        ReflectionTestUtils.setField(request, "socialWeight", socialWeight);
        ReflectionTestUtils.setField(request, "financeWeight", financeWeight);
        return request;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }
}
