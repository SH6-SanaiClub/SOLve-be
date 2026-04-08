package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.policy.entity.PointPolicy;
import com.shinhan.esg_be.domain.policy.entity.enums.PointPolicyType;
import com.shinhan.esg_be.domain.policy.repository.PointPolicyRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MonthlyPointServiceTest {

    @Autowired
    private MonthlyPointService monthlyPointService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointPolicyRepository pointPolicyRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        pointPolicyRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("월 마감 시 전월 매일 퀴즈 참여 사용자에게 월간 보너스를 지급한다")
    void settleMonthlyQuizBonus() {
        User user = userRepository.save(createUser(580));
        pointPolicyRepository.save(createPointPolicy(PointPolicyType.QUIZ_MONTHLY, PointReason.QUIZ_CORRECT, null, null, 1000));

        for (int day = 1; day <= 30; day++) {
            saveUserPoint(
                    user,
                    day % 2 == 0 ? PointReason.QUIZ_CORRECT : PointReason.QUIZ_WRONG,
                    20,
                    20L * day,
                    LocalDateTime.of(2026, 4, day, 9, 0)
            );
        }

        int bonusPoint = monthlyPointService.settleMonthlyQuizBonus(user.getUserId(), LocalDateTime.of(2026, 5, 1, 0, 7));

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<UserPoint> userPoints = userPointRepository.findAll();

        assertThat(bonusPoint).isEqualTo(1000);
        assertThat(savedUser.getTotalPoints()).isEqualTo(1580);
        assertThat(userPoints.stream().filter(point -> point.getReason() == PointReason.QUIZ_MONTHLY_BONUS)).hasSize(1);
    }

    private User createUser(int totalPoints) {
        User user = instantiate(User.class);
        ReflectionTestUtils.setField(user, "loginId", "monthly-point-user-" + totalPoints + "-" + System.nanoTime());
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "name", "tester");
        ReflectionTestUtils.setField(user, "phoneNumber", "01012345678");
        ReflectionTestUtils.setField(user, "birthdate", LocalDate.of(2000, 1, 1));
        ReflectionTestUtils.setField(user, "email", "monthly" + totalPoints + System.nanoTime() + "@test.com");
        ReflectionTestUtils.setField(user, "ciDi", "CI-MONTHLY-" + totalPoints + System.nanoTime());
        ReflectionTestUtils.setField(user, "totalPoints", totalPoints);
        return user;
    }

    private PointPolicy createPointPolicy(
            PointPolicyType policyType,
            PointReason targetReason,
            Integer targetCount,
            Integer targetDays,
            Integer rewardPoint
    ) {
        PointPolicy pointPolicy = instantiate(PointPolicy.class);
        ReflectionTestUtils.setField(pointPolicy, "policyType", policyType);
        ReflectionTestUtils.setField(pointPolicy, "targetReason", targetReason);
        ReflectionTestUtils.setField(pointPolicy, "targetCount", targetCount);
        ReflectionTestUtils.setField(pointPolicy, "targetDays", targetDays);
        ReflectionTestUtils.setField(pointPolicy, "rewardPoint", rewardPoint);
        ReflectionTestUtils.setField(pointPolicy, "isActive", true);
        return pointPolicy;
    }

    private void saveUserPoint(
            User user,
            PointReason reason,
            long changedAmount,
            long pointAfter,
            LocalDateTime createdAt
    ) {
        UserPoint savedUserPoint = userPointRepository.saveAndFlush(
                UserPoint.create(user, null, reason, changedAmount, pointAfter)
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
