package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.score.entity.ExpiredScoreHistory;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ExpiredScoreHistoryRepository;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
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
class ScoreExpirationServiceTest {

    @Autowired
    private ScoreExpirationService scoreExpirationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ValidScoreHistoryRepository validScoreHistoryRepository;

    @Autowired
    private ExpiredScoreHistoryRepository expiredScoreHistoryRepository;

    @Test
    @DisplayName("만료된 점수는 사용자 점수에서 차감하고 만료 이력으로 이동한다")
    void expireUserScores() {
        User user = userRepository.save(createUser("expire-user-1", 70, 250, 100, 100));
        validScoreHistoryRepository.save(
                ValidScoreHistory.create(
                        user,
                        ScoreCategory.E,
                        10,
                        ScoreReason.PHOTO,
                        LocalDateTime.of(2026, 4, 1, 0, 0),
                        70
                )
        );

        int expiredCount = scoreExpirationService.expireUserScores(
                user.getUserId(),
                LocalDateTime.of(2026, 4, 6, 0, 0)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<ValidScoreHistory> validScoreHistories = validScoreHistoryRepository.findAll();
        List<ExpiredScoreHistory> expiredScoreHistories = expiredScoreHistoryRepository.findAll();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(savedUser.getEScore()).isEqualTo(60);
        assertThat(validScoreHistories).isEmpty();
        assertThat(expiredScoreHistories).hasSize(1);
        assertThat(expiredScoreHistories.get(0).getReason()).isEqualTo(ScoreReason.PHOTO);
        assertThat(expiredScoreHistories.get(0).getScoreAfter()).isEqualTo(60);
    }

    @Test
    @DisplayName("만료 대상 점수가 없으면 아무 것도 처리하지 않는다")
    void expireUserScoresWithoutExpiredTarget() {
        User user = userRepository.save(createUser("expire-user-2", 70, 250, 100, 100));
        validScoreHistoryRepository.save(
                ValidScoreHistory.create(
                        user,
                        ScoreCategory.E,
                        10,
                        ScoreReason.PHOTO,
                        LocalDateTime.of(2026, 5, 1, 0, 0),
                        70
                )
        );

        int expiredCount = scoreExpirationService.expireUserScores(
                user.getUserId(),
                LocalDateTime.of(2026, 4, 6, 0, 0)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<ValidScoreHistory> validScoreHistories = validScoreHistoryRepository.findAll();
        List<ExpiredScoreHistory> expiredScoreHistories = expiredScoreHistoryRepository.findAll();

        assertThat(expiredCount).isZero();
        assertThat(savedUser.getEScore()).isEqualTo(70);
        assertThat(validScoreHistories).hasSize(1);
        assertThat(expiredScoreHistories).isEmpty();
    }

    @Test
    @DisplayName("초기 점수와 어뷰징 감점은 만료 대상에서 제외한다")
    void excludeInitialScoreAndAbuseFromExpiration() {
        User user = userRepository.save(createUser("expire-user-3", 45, 250, 100, 100));
        validScoreHistoryRepository.save(
                ValidScoreHistory.create(
                        user,
                        ScoreCategory.E,
                        5,
                        ScoreReason.ABUSE,
                        LocalDateTime.of(2026, 4, 1, 0, 0),
                        45
                )
        );
        validScoreHistoryRepository.save(
                ValidScoreHistory.create(
                        user,
                        ScoreCategory.E,
                        50,
                        ScoreReason.INITIAL_SCORE,
                        LocalDateTime.of(2026, 4, 1, 0, 0),
                        50
                )
        );

        int expiredCount = scoreExpirationService.expireUserScores(
                user.getUserId(),
                LocalDateTime.of(2026, 4, 6, 0, 0)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<ValidScoreHistory> validScoreHistories = validScoreHistoryRepository.findAll();
        List<ExpiredScoreHistory> expiredScoreHistories = expiredScoreHistoryRepository.findAll();

        assertThat(expiredCount).isZero();
        assertThat(savedUser.getEScore()).isEqualTo(45);
        assertThat(validScoreHistories).hasSize(2);
        assertThat(expiredScoreHistories).isEmpty();
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
