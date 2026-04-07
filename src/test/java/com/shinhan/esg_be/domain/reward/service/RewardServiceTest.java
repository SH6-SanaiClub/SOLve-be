package com.shinhan.esg_be.domain.reward.service;

import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.policy.entity.ActivityRewardPolicy;
import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.ActivityRewardPolicyRepository;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.reward.service.command.ApplyActivityRewardCommand;
import com.shinhan.esg_be.domain.reward.service.result.ApplyActivityRewardResult;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
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
class RewardServiceTest {

    @Autowired
    private RewardService rewardService;

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

    @Autowired
    private UserPointRepository userPointRepository;

    @Test
    @DisplayName("활동 보상 적용 시 점수와 포인트를 함께 반영한다")
    void applyActivityReward() {
        User user = userRepository.save(createUser("reward-user-1", 50, 250, 100, 100, 0));
        userMonthlyStatRepository.save(createUserMonthlyStat(user, 0, 0, 0));
        activityRewardPolicyRepository.save(
                createActivityRewardPolicy(ActivityType.DONATION, ScoreCategory.S, 20, null, new BigDecimal("0.03"))
        );
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.S, 500, 25, 3, 50, 250));

        ApplyActivityRewardResult result = rewardService.applyActivityReward(
                new ApplyActivityRewardCommand(
                        user.getUserId(),
                        ActivityType.DONATION,
                        BigDecimal.valueOf(30000),
                        LocalDateTime.of(2026, 4, 7, 10, 0)
                )
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        UserMonthlyStat savedStat = userMonthlyStatRepository.findByUser(savedUser).orElseThrow();
        List<ValidScoreHistory> scoreHistories = validScoreHistoryRepository.findAll();
        List<UserPoint> userPoints = userPointRepository.findAll();

        assertThat(result.scoreResult().appliedScore()).isEqualTo(20);
        assertThat(result.scoreResult().scoreAfter()).isEqualTo(270);
        assertThat(result.pointResult().activityPoint()).isEqualTo(900);
        assertThat(result.pointResult().bonusPoint()).isZero();
        assertThat(result.pointResult().pointAfter()).isEqualTo(900);

        assertThat(savedUser.getSScore()).isEqualTo(270);
        assertThat(savedUser.getTotalPoints()).isEqualTo(900);
        assertThat(savedStat.getMonthlySScore()).isEqualTo(20);

        assertThat(scoreHistories).hasSize(1);
        assertThat(scoreHistories.get(0).getReason()).isEqualTo(ScoreReason.DONATION);

        assertThat(userPoints).hasSize(1);
        assertThat(userPoints.get(0).getReason()).isEqualTo(PointReason.DONATION);
    }

    @Test
    @DisplayName("대출 상환 보상은 점수만 반영하고 포인트는 적립하지 않는다")
    void applyLoanRepayRewardWithoutPoint() {
        User user = userRepository.save(createUser("reward-user-2", 50, 250, 100, 100, 0));
        userMonthlyStatRepository.save(createUserMonthlyStat(user, 0, 0, 0));
        activityRewardPolicyRepository.save(
                createActivityRewardPolicy(ActivityType.LOAN_REPAY, ScoreCategory.G_REPAYMENT, 100, 0, BigDecimal.ZERO)
        );
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.G_REPAYMENT, 200, 0, 0, 0, 100));

        ApplyActivityRewardResult result = rewardService.applyActivityReward(
                new ApplyActivityRewardCommand(
                        user.getUserId(),
                        ActivityType.LOAN_REPAY,
                        null,
                        LocalDateTime.of(2026, 4, 7, 10, 0)
                )
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<ValidScoreHistory> scoreHistories = validScoreHistoryRepository.findAll();
        List<UserPoint> userPoints = userPointRepository.findAll();

        assertThat(result.scoreResult().appliedScore()).isEqualTo(100);
        assertThat(result.pointResult().pointReason()).isNull();
        assertThat(result.pointResult().activityPoint()).isZero();
        assertThat(result.pointResult().bonusPoint()).isZero();
        assertThat(result.pointResult().pointAfter()).isZero();

        assertThat(savedUser.getGRepaymentScore()).isEqualTo(200);
        assertThat(savedUser.getTotalPoints()).isZero();
        assertThat(scoreHistories).hasSize(1);
        assertThat(scoreHistories.get(0).getReason()).isEqualTo(ScoreReason.LOAN_REPAY);
        assertThat(userPoints).isEmpty();
    }

    private User createUser(
            String loginId,
            int eScore,
            int sScore,
            int gActivityScore,
            int gRepaymentScore,
            int totalPoints
    ) {
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
        ReflectionTestUtils.setField(user, "totalPoints", totalPoints);
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

    private ActivityRewardPolicy createActivityRewardPolicy(
            ActivityType activityType,
            ScoreCategory scoreCategory,
            int scoreValue,
            Integer pointValue,
            BigDecimal pointRate
    ) {
        ActivityRewardPolicy activityRewardPolicy = newInstance(ActivityRewardPolicy.class);
        ReflectionTestUtils.setField(activityRewardPolicy, "activityType", activityType);
        ReflectionTestUtils.setField(activityRewardPolicy, "scoreCategory", scoreCategory);
        ReflectionTestUtils.setField(activityRewardPolicy, "scoreValue", scoreValue);
        ReflectionTestUtils.setField(activityRewardPolicy, "pointValue", pointValue);
        ReflectionTestUtils.setField(activityRewardPolicy, "pointRate", pointRate);
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
