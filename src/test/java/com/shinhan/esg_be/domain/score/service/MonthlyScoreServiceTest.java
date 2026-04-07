package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.score.service.result.MonthlyScoreSettlementResult;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MonthlyScoreServiceTest {

    @Autowired
    private MonthlyScoreService monthlyScoreService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMonthlyStatRepository userMonthlyStatRepository;

    @Autowired
    private EsgScorePolicyRepository esgScorePolicyRepository;

    @Autowired
    private ValidScoreHistoryRepository validScoreHistoryRepository;

    @Test
    @DisplayName("연속 달성 목표를 채우면 보너스 점수를 지급한다")
    void settleMonthlyScoreWithBonus() {
        User user = userRepository.save(createUser("monthly-user-1", 50, 250, 100, 100));
        userMonthlyStatRepository.save(createUserMonthlyStat(user, 5, 25, 10, 2, 1, 2));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.E, 100, 5, 3, 10, 50));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.S, 500, 25, 3, 50, 250));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.G_ACTIVITY, 200, 10, 3, 10, 100));

        MonthlyScoreSettlementResult result = monthlyScoreService.settleMonthlyScore(
                user.getUserId(),
                LocalDateTime.of(2026, 4, 30, 23, 59)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        UserMonthlyStat savedStat = userMonthlyStatRepository.findByUser(savedUser).orElseThrow();
        List<ValidScoreHistory> scoreHistories = validScoreHistoryRepository.findAll();

        assertThat(result.bonusCount()).isEqualTo(2);
        assertThat(savedUser.getEScore()).isEqualTo(60);
        assertThat(savedUser.getGActivityScore()).isEqualTo(110);
        assertThat(savedStat.getConsecutiveEMaxScore()).isZero();
        assertThat(savedStat.getConsecutiveSMaxScore()).isEqualTo(2);
        assertThat(savedStat.getConsecutiveGMaxScore()).isZero();
        assertThat(savedStat.getMonthlyEScore()).isZero();
        assertThat(savedStat.getMonthlySScore()).isZero();
        assertThat(savedStat.getMonthlyGScore()).isZero();
        assertThat(scoreHistories).hasSize(2);
        assertThat(scoreHistories).allMatch(history -> history.getReason() == ScoreReason.CONSECUTIVE_BONUS);
    }

    @Test
    @DisplayName("월 최대 점수를 채우지 못하면 보너스를 지급하지 않는다")
    void settleMonthlyScoreWithoutBonus() {
        User user = userRepository.save(createUser("monthly-user-2", 50, 250, 100, 100));
        userMonthlyStatRepository.save(createUserMonthlyStat(user, 4, 10, 8, 2, 2, 1));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.E, 100, 5, 3, 10, 50));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.S, 500, 25, 3, 50, 250));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.G_ACTIVITY, 200, 10, 3, 20, 100));

        MonthlyScoreSettlementResult result = monthlyScoreService.settleMonthlyScore(
                user.getUserId(),
                LocalDateTime.of(2026, 4, 30, 23, 59)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        UserMonthlyStat savedStat = userMonthlyStatRepository.findByUser(savedUser).orElseThrow();

        assertThat(result.bonusCount()).isZero();
        assertThat(savedUser.getEScore()).isEqualTo(50);
        assertThat(savedUser.getSScore()).isEqualTo(250);
        assertThat(savedUser.getGActivityScore()).isEqualTo(100);
        assertThat(savedStat.getConsecutiveEMaxScore()).isZero();
        assertThat(savedStat.getConsecutiveSMaxScore()).isZero();
        assertThat(savedStat.getConsecutiveGMaxScore()).isZero();
        assertThat(savedStat.getMonthlyEScore()).isZero();
        assertThat(savedStat.getMonthlySScore()).isZero();
        assertThat(savedStat.getMonthlyGScore()).isZero();
        assertThat(validScoreHistoryRepository.findAll()).isEmpty();
    }

    private User createUser(String loginId, int eScore, int sScore, int gActivityScore, int gRepaymentScore) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "loginId", loginId);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "name", "tester");
        ReflectionTestUtils.setField(user, "phoneNumber", "01012345678");
        ReflectionTestUtils.setField(user, "birthdate", LocalDateTime.of(2000, 1, 1, 0, 0));
        ReflectionTestUtils.setField(user, "email", loginId + "@test.com");
        ReflectionTestUtils.setField(user, "ciDi", "ci-di-" + loginId);
        ReflectionTestUtils.setField(user, "userType", UserType.ALL_ROUNDER);
        ReflectionTestUtils.setField(user, "abuseCount", 0);
        ReflectionTestUtils.setField(user, "eScore", eScore);
        ReflectionTestUtils.setField(user, "sScore", sScore);
        ReflectionTestUtils.setField(user, "gActivityScore", gActivityScore);
        ReflectionTestUtils.setField(user, "gRepaymentScore", gRepaymentScore);
        ReflectionTestUtils.setField(user, "currentGrade", Grade.SEED);
        ReflectionTestUtils.setField(user, "totalPoints", 0);
        ReflectionTestUtils.setField(user, "isActive", true);
        ReflectionTestUtils.setField(user, "isLinked", false);
        return user;
    }

    private UserMonthlyStat createUserMonthlyStat(
            User user,
            int monthlyEScore,
            int monthlySScore,
            int monthlyGScore,
            int consecutiveEMaxScore,
            int consecutiveSMaxScore,
            int consecutiveGMaxScore
    ) {
        UserMonthlyStat userMonthlyStat = UserMonthlyStat.create(user);
        ReflectionTestUtils.setField(userMonthlyStat, "monthlyEScore", monthlyEScore);
        ReflectionTestUtils.setField(userMonthlyStat, "monthlySScore", monthlySScore);
        ReflectionTestUtils.setField(userMonthlyStat, "monthlyGScore", monthlyGScore);
        ReflectionTestUtils.setField(userMonthlyStat, "consecutiveEMaxScore", consecutiveEMaxScore);
        ReflectionTestUtils.setField(userMonthlyStat, "consecutiveSMaxScore", consecutiveSMaxScore);
        ReflectionTestUtils.setField(userMonthlyStat, "consecutiveGMaxScore", consecutiveGMaxScore);
        return userMonthlyStat;
    }

    private EsgScorePolicy createEsgScorePolicy(
            ScoreCategory scoreCategory,
            int maxScore,
            int monthlyMaxScore,
            int consecutiveTargetMonths,
            int consecutiveBonusScore,
            int baseScore
    ) {
        EsgScorePolicy esgScorePolicy = newInstance(EsgScorePolicy.class);
        ReflectionTestUtils.setField(esgScorePolicy, "category", scoreCategory);
        ReflectionTestUtils.setField(esgScorePolicy, "monthlyMaxScore", monthlyMaxScore);
        ReflectionTestUtils.setField(esgScorePolicy, "maxScore", maxScore);
        ReflectionTestUtils.setField(esgScorePolicy, "consecutiveTargetMonths", consecutiveTargetMonths);
        ReflectionTestUtils.setField(esgScorePolicy, "consecutiveBonusScore", consecutiveBonusScore);
        ReflectionTestUtils.setField(esgScorePolicy, "isActive", true);
        ReflectionTestUtils.setField(esgScorePolicy, "baseScore", baseScore);
        return esgScorePolicy;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create test instance: " + type.getSimpleName(), exception);
        }
    }
}
