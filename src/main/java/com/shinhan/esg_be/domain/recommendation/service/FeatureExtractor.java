package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.activity.repository.UserActivityRepository;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.social.repository.UserEcoProductRepository;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import com.shinhan.esg_be.domain.quiz.repository.UserQuizRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeatureExtractor {

    private final UserRepository              userRepository;
    private final UserMonthlyStatRepository   userMonthlyStatRepository;
    private final UserActivityRepository      userActivityRepository;
    private final UserDonationRepository      userDonationRepository;
    private final UserVolunteerRepository     userVolunteerRepository;
    private final UserEcoProductRepository    userEcoProductRepository;
    private final UserQuizRepository          userQuizRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;
    private final UserSavingRepository        userSavingRepository;
    private final UserLoanRepository          userLoanRepository;

    public UserFeatureDto extract(Long userId) {
        log.debug("FeatureExtractor.extract() userId={}", userId);

        // 1. 사용자 기본 정보
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 사용자입니다. userId=" + userId));

        // 2. 이번 달 카테고리별 획득 점수 (월 한도 필터용)
        YearMonth now = YearMonth.now();
        var monthlyStat = userMonthlyStatRepository
                .findByUserAndMonth(userId, now.getYear(), now.getMonthValue());

        int monthlyEScore = monthlyStat.map(s -> s.getMonthlyEScore()).orElse(0);
        int monthlySScore = monthlyStat.map(s -> s.getMonthlySScore()).orElse(0);
        int monthlyGScore = monthlyStat.map(s -> s.getMonthlyGScore()).orElse(0);

        // 3. 최근 90일 / 30일 기준 시각
        LocalDateTime since90 = LocalDateTime.now().minusDays(90);
        LocalDateTime since30 = LocalDateTime.now().minusDays(30);

        // 4. 최근 90일 카테고리별 활동 횟수 (C1, C2용)
        int recentECount = (int) userActivityRepository
                .countApprovedSince(userId, since90);
        int recentSCount = (int) (
                userDonationRepository.countSince(userId, since90) +
                        userVolunteerRepository.countCompletedSince(userId, since90) +
                        userEcoProductRepository.countSince(userId, since90)
        );
        int recentGCount = (int) userQuizRepository
                .countSince(userId, since90);

        // 5. 미활동 패널티 위험 여부 (최근 30일 활동 없음)
        long totalRecent30 =
                userActivityRepository.countApprovedSince(userId, since30) +
                        userDonationRepository.countSince(userId, since30) +
                        userVolunteerRepository.countCompletedSince(userId, since30) +
                        userEcoProductRepository.countSince(userId, since30) +
                        userQuizRepository.countSince(userId, since30);

        boolean inactivityRisk = (totalRecent30 == 0);

        // 6. 유효점수 만료 임박 여부 (30일 이내)
        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime expiryThreshold = nowDateTime.plusDays(30);
        boolean scoreExpiryRisk = validScoreHistoryRepository
                .existsByUser_UserIdAndValidUntilBetween(userId, nowDateTime, expiryThreshold);

        // 7. 금융 상품 상태 (B3 금융연계도용)
        var activeSaving = userSavingRepository
                .findByUser_UserIdAndStatus(userId, SavingStatus.ACTIVE);
        var activeLoan = userLoanRepository
                .findByUser_UserIdAndStatus(userId, LoanStatus.ACTIVE);

        boolean hasSaving        = activeSaving.isPresent();
        boolean hasLoan          = activeLoan.isPresent();
        LocalDate savingMaturityDate = activeSaving
                .map(UserSaving::getMaturityDate).orElse(null);
        Long savingFinProductId  = activeSaving
                .map(s -> s.getFinancialProduct().getFinProductId()).orElse(null);

        return UserFeatureDto.builder()
                .userId(userId)
                .userType(user.getUserType())
                .currentGrade(user.getCurrentGrade())
                .eScore(user.getEScore())
                .sScore(user.getSScore())
                .gActivityScore(user.getGActivityScore())
                .gRepaymentScore(user.getGRepaymentScore())
                .monthlyEScore(monthlyEScore)
                .monthlySScore(monthlySScore)
                .monthlyGScore(monthlyGScore)
                .recentECount(recentECount)
                .recentSCount(recentSCount)
                .recentGCount(recentGCount)
                .inactivityRisk(inactivityRisk)
                .scoreExpiryRisk(scoreExpiryRisk)
                .hasSaving(hasSaving)
                .hasLoan(hasLoan)
                .savingMaturityDate(savingMaturityDate)
                .savingFinProductId(savingFinProductId)
                .build();
    }
}
