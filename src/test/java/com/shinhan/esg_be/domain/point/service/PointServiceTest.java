package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.EsgBeApplication;
import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointCategory;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.point.service.command.ApplyActivityPointCommand;
import com.shinhan.esg_be.domain.point.service.result.ApplyActivityPointResult;
import com.shinhan.esg_be.domain.policy.entity.ActivityRewardPolicy;
import com.shinhan.esg_be.domain.policy.entity.PointPolicy;
import com.shinhan.esg_be.domain.policy.entity.enums.PointPolicyType;
import com.shinhan.esg_be.domain.policy.repository.ActivityRewardPolicyRepository;
import com.shinhan.esg_be.domain.policy.repository.PointPolicyRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {EsgBeApplication.class, PointServiceTest.PointServiceTestConfig.class})
@Transactional
class PointServiceTest {

    @TestConfiguration
    static class PointServiceTestConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-04-30T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private PointService pointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRewardPolicyRepository activityRewardPolicyRepository;

    @Autowired
    private PointPolicyRepository pointPolicyRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("기부 포인트를 비율로 반올림 적립한다")
    void applyDonationPointWithRoundedRate() {
        User user = userRepository.save(createUser(0));
        activityRewardPolicyRepository.save(createActivityRewardPolicy(ActivityType.DONATION, ScoreCategory.S, 20, null, new BigDecimal("0.03")));

        ApplyActivityPointResult result = pointService.applyActivityPoint(
                new ApplyActivityPointCommand(user.getUserId(), ActivityType.DONATION, new BigDecimal("1050"), LocalDateTime.of(2026, 4, 6, 10, 0), null)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<UserPoint> userPoints = userPointRepository.findAll();

        assertThat(result.activityPoint()).isEqualTo(32);
        assertThat(result.bonusPoint()).isZero();
        assertThat(result.pointAfter()).isEqualTo(32);
        assertThat(savedUser.getTotalPoints()).isEqualTo(32);
        assertThat(userPoints).hasSize(1);
        assertThat(userPoints.get(0).getCategory()).isEqualTo(PointCategory.DONATION);
    }

    @Test
    @DisplayName("퀴즈 정답과 오답은 각각의 활동 보상 정책을 사용한다")
    void applyQuizPointByActivityType() {
        User user = userRepository.save(createUser(0));
        activityRewardPolicyRepository.save(createActivityRewardPolicy(ActivityType.QUIZ_CORRECT, ScoreCategory.G_ACTIVITY, 1, 20, null));
        activityRewardPolicyRepository.save(createActivityRewardPolicy(ActivityType.QUIZ_WRONG, ScoreCategory.G_ACTIVITY, 0, 10, null));

        ApplyActivityPointResult correctResult = pointService.applyActivityPoint(
                new ApplyActivityPointCommand(user.getUserId(), ActivityType.QUIZ_CORRECT, null, LocalDateTime.of(2026, 4, 6, 9, 0), null)
        );
        ApplyActivityPointResult wrongResult = pointService.applyActivityPoint(
                new ApplyActivityPointCommand(user.getUserId(), ActivityType.QUIZ_WRONG, null, LocalDateTime.of(2026, 5, 1, 9, 0), null)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();

        assertThat(correctResult.activityPoint()).isEqualTo(20);
        assertThat(wrongResult.activityPoint()).isEqualTo(10);
        assertThat(savedUser.getTotalPoints()).isEqualTo(30);
    }

    @Test
    @DisplayName("봉사 5회째에는 포인트 정책 기준으로 보너스를 지급한다")
    void grantVolunteerMilestoneBonus() {
        User user = userRepository.save(createUser(4000));
        activityRewardPolicyRepository.save(createActivityRewardPolicy(ActivityType.VOLUNTEER, ScoreCategory.S, 5, 1000, null));
        pointPolicyRepository.save(createPointPolicy(PointPolicyType.VOLUNTEER_MILESTONE, PointCategory.VOLUNTEER, 5, null, 1000));

        for (int i = 0; i < 4; i++) {
            saveUserPoint(user, PointCategory.VOLUNTEER, 1000, 1000L * (i + 1), LocalDateTime.of(2026, 4, i + 1, 9, 0));
        }

        ApplyActivityPointResult result = pointService.applyActivityPoint(
                new ApplyActivityPointCommand(user.getUserId(), ActivityType.VOLUNTEER, null, LocalDateTime.of(2026, 4, 6, 12, 0), null)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<UserPoint> userPoints = userPointRepository.findAll();

        assertThat(result.activityPoint()).isEqualTo(1000);
        assertThat(result.bonusPoint()).isEqualTo(1000);
        assertThat(savedUser.getTotalPoints()).isEqualTo(6000);
        assertThat(userPoints).hasSize(6);
        assertThat(userPoints.get(userPoints.size() - 1).getCategory()).isEqualTo(PointCategory.VOLUNTEER_MILESTONE_BONUS);
    }

    @Test
    @DisplayName("사진은 같은 날 여러 번 적립되지만 7일 연속 보너스는 포인트 정책 기준으로 지급한다")
    void grantPhotoStreakBonusOnlyOncePerDay() {
        User user = userRepository.save(createUser(180));
        activityRewardPolicyRepository.save(createActivityRewardPolicy(ActivityType.PHOTO, ScoreCategory.E, 1, 30, null));
        pointPolicyRepository.save(createPointPolicy(PointPolicyType.PHOTO_STREAK, PointCategory.PHOTO, null, 7, 300));

        LocalDateTime today = LocalDateTime.of(2026, 4, 30, 10, 0);
        for (int i = 6; i >= 1; i--) {
            saveUserPoint(user, PointCategory.PHOTO, 30, 180 + (30L * (7 - i)), today.minusDays(i));
        }

        ApplyActivityPointResult firstResult = pointService.applyActivityPoint(
                new ApplyActivityPointCommand(user.getUserId(), ActivityType.PHOTO, null, today, null)
        );
        ApplyActivityPointResult secondResult = pointService.applyActivityPoint(
                new ApplyActivityPointCommand(user.getUserId(), ActivityType.PHOTO, null, today.plusHours(1), null)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<UserPoint> userPoints = userPointRepository.findAll();

        assertThat(firstResult.activityPoint()).isEqualTo(30);
        assertThat(firstResult.bonusPoint()).isEqualTo(300);
        assertThat(secondResult.activityPoint()).isEqualTo(30);
        assertThat(secondResult.bonusPoint()).isZero();
        assertThat(savedUser.getTotalPoints()).isEqualTo(540);
        assertThat(userPoints.stream().filter(point -> point.getCategory() == PointCategory.PHOTO_STREAK_BONUS)).hasSize(1);
    }

    @Test
    @DisplayName("해당 달 매일 퀴즈에 참여하면 월간 보너스를 지급한다")
    void grantMonthlyQuizBonusWhenParticipatedEveryDay() {
        User user = userRepository.save(createUser(580));
        activityRewardPolicyRepository.save(createActivityRewardPolicy(ActivityType.QUIZ_CORRECT, ScoreCategory.G_ACTIVITY, 1, 20, null));
        pointPolicyRepository.save(createPointPolicy(PointPolicyType.QUIZ_MONTHLY, PointCategory.QUIZ_CORRECT, null, null, 1000));

        YearMonth yearMonth = YearMonth.of(2026, 4);
        long pointAfter = 0;
        for (int day = 1; day < yearMonth.lengthOfMonth(); day++) {
            pointAfter += 20;
            saveUserPoint(
                    user,
                    day % 2 == 0 ? PointCategory.QUIZ_CORRECT : PointCategory.QUIZ_WRONG,
                    20,
                    pointAfter,
                    LocalDateTime.of(2026, 4, day, 9, 0)
            );
        }

        ApplyActivityPointResult result = pointService.applyActivityPoint(
                new ApplyActivityPointCommand(user.getUserId(), ActivityType.QUIZ_CORRECT, null, LocalDateTime.of(2026, 4, 30, 9, 0), null)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();

        assertThat(result.activityPoint()).isEqualTo(20);
        assertThat(result.bonusPoint()).isEqualTo(1000);
        assertThat(savedUser.getTotalPoints()).isEqualTo(1600);
        assertThat(userPointRepository.findAll().stream().filter(point -> point.getCategory() == PointCategory.QUIZ_MONTHLY_BONUS)).hasSize(1);
    }

    @Test
    @DisplayName("어뷰징 환수는 마이너스 포인트 이력으로 남긴다")
    void reclaimAbusedPoint() {
        User user = userRepository.save(createUser(5000));

        int pointAfter = pointService.reclaimAbusedPoint(user.getUserId(), 1200);

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<UserPoint> userPoints = userPointRepository.findAll();

        assertThat(pointAfter).isEqualTo(3800);
        assertThat(savedUser.getTotalPoints()).isEqualTo(3800);
        assertThat(userPoints).hasSize(1);
        assertThat(userPoints.get(0).getCategory()).isEqualTo(PointCategory.ABUSE_RECLAIM);
        assertThat(userPoints.get(0).getChangedAmount()).isEqualTo(-1200L);
    }

    private User createUser(int totalPoints) {
        User user = instantiate(User.class);
        ReflectionTestUtils.setField(user, "loginId", "point-user-" + totalPoints + "-" + System.nanoTime());
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "name", "포인트유저");
        ReflectionTestUtils.setField(user, "phoneNumber", "01012345678");
        ReflectionTestUtils.setField(user, "birthdate", LocalDateTime.of(2000, 1, 1, 0, 0));
        ReflectionTestUtils.setField(user, "email", "point" + totalPoints + System.nanoTime() + "@test.com");
        ReflectionTestUtils.setField(user, "ciDi", "CI" + totalPoints + System.nanoTime());
        ReflectionTestUtils.setField(user, "totalPoints", totalPoints);
        return user;
    }

    private ActivityRewardPolicy createActivityRewardPolicy(
            ActivityType activityType,
            ScoreCategory scoreCategory,
            int scoreValue,
            Integer pointValue,
            BigDecimal pointRate
    ) {
        ActivityRewardPolicy policy = instantiate(ActivityRewardPolicy.class);
        ReflectionTestUtils.setField(policy, "activityType", activityType);
        ReflectionTestUtils.setField(policy, "scoreCategory", scoreCategory);
        ReflectionTestUtils.setField(policy, "scoreValue", scoreValue);
        ReflectionTestUtils.setField(policy, "pointValue", pointValue);
        ReflectionTestUtils.setField(policy, "pointRate", pointRate);
        return policy;
    }

    private PointPolicy createPointPolicy(
            PointPolicyType policyType,
            PointCategory targetCategory,
            Integer targetCount,
            Integer targetDays,
            Integer rewardPoint
    ) {
        PointPolicy pointPolicy = instantiate(PointPolicy.class);
        ReflectionTestUtils.setField(pointPolicy, "policyType", policyType);
        ReflectionTestUtils.setField(pointPolicy, "targetCategory", targetCategory);
        ReflectionTestUtils.setField(pointPolicy, "targetCount", targetCount);
        ReflectionTestUtils.setField(pointPolicy, "targetDays", targetDays);
        ReflectionTestUtils.setField(pointPolicy, "rewardPoint", rewardPoint);
        ReflectionTestUtils.setField(pointPolicy, "isActive", true);
        return pointPolicy;
    }

    private void saveUserPoint(
            User user,
            PointCategory category,
            long changedAmount,
            long pointAfter,
            LocalDateTime createdAt
    ) {
        UserPoint savedUserPoint = userPointRepository.saveAndFlush(
                UserPoint.create(user, null, category, changedAmount, pointAfter)
        );

        entityManager.createNativeQuery("update user_point set created_at = :createdAt where exchange_id = :exchangeId")
                .setParameter("createdAt", createdAt)
                .setParameter("exchangeId", savedUserPoint.getExchangeId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }
}
