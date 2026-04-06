package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.policy.entity.ActivityRewardPolicy;
import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.ActivityRewardPolicyRepository;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.score.service.command.ApplyActivityScoreCommand;
import com.shinhan.esg_be.domain.score.service.result.ApplyActivityScoreResult;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ScoreServiceTest {

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMonthlyStatRepository userMonthlyStatRepository;

    @Autowired
    private ActivityRewardPolicyRepository activityRewardPolicyRepository;

    @Autowired
    private EsgScorePolicyRepository esgScorePolicyRepository;

    @Autowired
    private ValidScoreHistoryRepository validScoreHistoryRepository;

    @Test
    @DisplayName("활동 점수를 정상 반영한다")
    void applyActivityScore() {
        User user = userRepository.save(createUser("score-user-1", 50, 250, 100, 100));
        userMonthlyStatRepository.save(createUserMonthlyStat(user, 0, 0, 0));
        activityRewardPolicyRepository.save(createActivityRewardPolicy(ActivityType.DONATION, ScoreCategory.S, 20));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.S, 500, 25, 3, 50, 250));

        ApplyActivityScoreResult result = scoreService.applyActivityScore(
                new ApplyActivityScoreCommand(
                        user.getUserId(),
                        ActivityType.DONATION,
                        BigDecimal.valueOf(30000),
                        LocalDateTime.of(2026, 4, 6, 10, 0)
                )
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        UserMonthlyStat savedStat = userMonthlyStatRepository.findByUser(savedUser).orElseThrow();
        List<ValidScoreHistory> scoreHistories = validScoreHistoryRepository.findAll();

        assertThat(result.appliedScore()).isEqualTo(20);
        assertThat(result.scoreAfter()).isEqualTo(270);
        assertThat(savedUser.getSScore()).isEqualTo(270);
        assertThat(savedStat.getMonthlySScore()).isEqualTo(20);
        assertThat(scoreHistories).hasSize(1);
        assertThat(scoreHistories.get(0).getReason()).isEqualTo(ScoreReason.DONATION);
    }

    @Test
    @DisplayName("월 최대 점수를 넘기면 남은 점수만 반영한다")
    void applyRemainingScoreWithinMonthlyLimit() {
        User user = userRepository.save(createUser("score-user-2", 50, 250, 100, 100));
        userMonthlyStatRepository.save(createUserMonthlyStat(user, 4, 0, 0));
        activityRewardPolicyRepository.save(createActivityRewardPolicy(ActivityType.PHOTO, ScoreCategory.E, 3));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.E, 100, 5, 3, 10, 50));

        ApplyActivityScoreResult result = scoreService.applyActivityScore(
                new ApplyActivityScoreCommand(
                        user.getUserId(),
                        ActivityType.PHOTO,
                        null,
                        LocalDateTime.of(2026, 4, 6, 10, 0)
                )
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        UserMonthlyStat savedStat = userMonthlyStatRepository.findByUser(savedUser).orElseThrow();

        assertThat(result.appliedScore()).isEqualTo(1);
        assertThat(result.cappedByMonthlyLimit()).isTrue();
        assertThat(savedUser.getEScore()).isEqualTo(51);
        assertThat(savedStat.getMonthlyEScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("전체 최대 점수를 넘기면 남은 점수만 반영한다")
    void applyRemainingScoreWithinTotalLimit() {
        User user = userRepository.save(createUser("score-user-3", 95, 250, 100, 100));
        userMonthlyStatRepository.save(createUserMonthlyStat(user, 0, 0, 0));
        activityRewardPolicyRepository.save(createActivityRewardPolicy(ActivityType.PHOTO, ScoreCategory.E, 10));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.E, 100, 5, 3, 10, 50));

        ApplyActivityScoreResult result = scoreService.applyActivityScore(
                new ApplyActivityScoreCommand(
                        user.getUserId(),
                        ActivityType.PHOTO,
                        null,
                        LocalDateTime.of(2026, 4, 6, 10, 0)
                )
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        UserMonthlyStat savedStat = userMonthlyStatRepository.findByUser(savedUser).orElseThrow();

        assertThat(result.appliedScore()).isEqualTo(5);
        assertThat(result.cappedByMonthlyLimit()).isTrue();
        assertThat(savedUser.getEScore()).isEqualTo(100);
        assertThat(savedStat.getMonthlyEScore()).isEqualTo(5);
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

    private UserMonthlyStat createUserMonthlyStat(User user, int monthlyEScore, int monthlySScore, int monthlyGScore) {
        UserMonthlyStat userMonthlyStat = UserMonthlyStat.create(user);
        ReflectionTestUtils.setField(userMonthlyStat, "monthlyEScore", monthlyEScore);
        ReflectionTestUtils.setField(userMonthlyStat, "monthlySScore", monthlySScore);
        ReflectionTestUtils.setField(userMonthlyStat, "monthlyGScore", monthlyGScore);
        return userMonthlyStat;
    }

    private ActivityRewardPolicy createActivityRewardPolicy(ActivityType activityType, ScoreCategory scoreCategory, int scoreValue) {
        ActivityRewardPolicy activityRewardPolicy = newInstance(ActivityRewardPolicy.class);
        ReflectionTestUtils.setField(activityRewardPolicy, "activityType", activityType);
        ReflectionTestUtils.setField(activityRewardPolicy, "scoreCategory", scoreCategory);
        ReflectionTestUtils.setField(activityRewardPolicy, "scoreValue", scoreValue);
        ReflectionTestUtils.setField(activityRewardPolicy, "pointValue", 0);
        ReflectionTestUtils.setField(activityRewardPolicy, "pointRate", BigDecimal.ZERO);
        return activityRewardPolicy;
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
