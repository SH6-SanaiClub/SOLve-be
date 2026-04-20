package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.quiz.repository.UserQuizRepository;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.social.repository.UserEcoProductRepository;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.environment.repository.UserEnvironmentActivityRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureExtractorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMonthlyStatRepository userMonthlyStatRepository;

    @Mock
    private UserEnvironmentActivityRepository userEnvironmentActivityRepository;

    @Mock
    private UserDonationRepository userDonationRepository;

    @Mock
    private UserVolunteerRepository userVolunteerRepository;

    @Mock
    private UserEcoProductRepository userEcoProductRepository;

    @Mock
    private UserQuizRepository userQuizRepository;

    @Mock
    private ValidScoreHistoryRepository validScoreHistoryRepository;

    @Mock
    private UserSavingRepository userSavingRepository;

    @Mock
    private UserLoanRepository userLoanRepository;

    @InjectMocks
    private FeatureExtractor featureExtractor;

    @Test
    @DisplayName("월 누적 점수는 현재 user_monthly_stat row에서 읽는다")
    void extract_readsMonthlyStatFromCurrentRow() {
        User user = org.mockito.Mockito.mock(User.class);
        UserMonthlyStat monthlyStat = org.mockito.Mockito.mock(UserMonthlyStat.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMonthlyStatRepository.findByUser(user)).thenReturn(Optional.of(monthlyStat));
        when(user.getUserType()).thenReturn(UserType.ALL_ROUNDER);
        when(user.getCurrentGrade()).thenReturn(Grade.SEED);
        when(user.getEScore()).thenReturn(50);
        when(user.getSScore()).thenReturn(250);
        when(user.getGActivityScore()).thenReturn(100);
        when(monthlyStat.getMonthlyEScore()).thenReturn(3);
        when(monthlyStat.getMonthlySScore()).thenReturn(7);
        when(monthlyStat.getMonthlyGScore()).thenReturn(2);

        when(userEnvironmentActivityRepository.countApprovedSince(anyLong(), any())).thenReturn(0L);
        when(userDonationRepository.countSince(anyLong(), any())).thenReturn(0L);
        when(userVolunteerRepository.countCompletedSince(anyLong(), any())).thenReturn(0L);
        when(userEcoProductRepository.countSince(anyLong(), any())).thenReturn(0L);
        when(userQuizRepository.countSince(anyLong(), any())).thenReturn(0L);
        when(userQuizRepository.countToday(anyLong(), any())).thenReturn(0L);
        when(validScoreHistoryRepository.existsByUser_UserIdAndValidUntilBetween(anyLong(), any(), any()))
                .thenReturn(false);
        when(userSavingRepository.findAllByUser_UserIdAndStatus(anyLong(), any())).thenReturn(List.of());
        when(userLoanRepository.existsByUser_UserIdAndStatus(anyLong(), any())).thenReturn(false);

        UserFeatureDto feature = featureExtractor.extract(1L);

        assertThat(feature.getMonthlyEScore()).isEqualTo(3);
        assertThat(feature.getMonthlySScore()).isEqualTo(7);
        assertThat(feature.getMonthlyGScore()).isEqualTo(2);
        verify(userMonthlyStatRepository).findByUser(user);
        verify(userMonthlyStatRepository, never()).findByUserAndMonth(anyLong(), any(), any());
    }
}
