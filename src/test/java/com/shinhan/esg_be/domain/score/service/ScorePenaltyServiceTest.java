package com.shinhan.esg_be.domain.score.service;

import com.shinhan.esg_be.domain.policy.entity.PenaltyPolicy;
import com.shinhan.esg_be.domain.policy.entity.enums.PenaltyType;
import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.policy.repository.PenaltyPolicyRepository;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.score.service.result.PenaltyApplicationResult;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
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
class ScorePenaltyServiceTest {

    @Autowired
    private ScorePenaltyService scorePenaltyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PenaltyPolicyRepository penaltyPolicyRepository;

    @Autowired
    private ValidScoreHistoryRepository validScoreHistoryRepository;

    @Autowired
    private EsgScorePolicyRepository esgScorePolicyRepository;

    @BeforeEach
    void setUp() {
        validScoreHistoryRepository.deleteAllInBatch();
        penaltyPolicyRepository.deleteAllInBatch();
        esgScorePolicyRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("관리자 어뷰징 패널티를 적용하면 점수 차감과 대출 차단을 반영한다")
    void applyAbusePenalty() {
        User user = userRepository.save(createUser("penalty-user-1", 50, 250, 100, 100));
        penaltyPolicyRepository.save(createPenaltyPolicy(PenaltyType.ABUSE, 5, 25, 10, true));
        seedPolicies();
        seedInitialScores(user);

        PenaltyApplicationResult result = scorePenaltyService.applyAbusePenalty(
                user.getUserId(),
                LocalDateTime.of(2026, 4, 7, 14, 0)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<ValidScoreHistory> histories = validScoreHistoryRepository.findAll();

        assertThat(result.applied()).isTrue();
        assertThat(result.appliedEReduction()).isEqualTo(5);
        assertThat(result.appliedSReduction()).isEqualTo(25);
        assertThat(result.appliedGActivityReduction()).isEqualTo(10);
        assertThat(result.loanBlocked()).isTrue();

        assertThat(savedUser.getEScore()).isEqualTo(45);
        assertThat(savedUser.getSScore()).isEqualTo(225);
        assertThat(savedUser.getGActivityScore()).isEqualTo(90);
        assertThat(savedUser.getGRepaymentScore()).isEqualTo(100);
        assertThat(savedUser.getAbuseCount()).isEqualTo(1);
        assertThat(savedUser.getIsLoanBlocked()).isTrue();
        assertThat(histories.stream().filter(history -> history.getReason() == ScoreReason.INITIAL_SCORE).count())
                .isEqualTo(4);
        assertThat(histories.stream().filter(history -> history.getReason() == ScoreReason.ABUSE).count())
                .isEqualTo(3);
        assertThat(histories.stream().filter(history -> history.getReason() == ScoreReason.ABUSE))
                .allMatch(history -> history.getValidUntil() == null);
    }

    @Test
    @DisplayName("무활동 패널티는 어뷰징으로 낮아진 기본 점수선 아래로는 내려가지 않는다")
    void applyNoActivityPenaltyWithoutDroppingBelowFloor() {
        User user = userRepository.save(createUser("penalty-user-2", 46, 235, 91, 100));
        penaltyPolicyRepository.save(createPenaltyPolicy(PenaltyType.NO_ACTIVITY, 2, 10, 4, false));
        seedPolicies();
        seedScoreFloor(user);
        ReflectionTestUtils.setField(user, "lastActivityDate", LocalDateTime.of(2026, 2, 1, 0, 0));

        PenaltyApplicationResult result = scorePenaltyService.applyNoActivityPenalty(
                user.getUserId(),
                LocalDateTime.of(2026, 4, 7, 14, 0)
        );

        User savedUser = userRepository.findById(user.getUserId()).orElseThrow();
        List<ValidScoreHistory> histories = validScoreHistoryRepository.findAll();

        assertThat(result.applied()).isTrue();
        assertThat(result.appliedEReduction()).isEqualTo(1);
        assertThat(result.appliedSReduction()).isEqualTo(10);
        assertThat(result.appliedGActivityReduction()).isEqualTo(1);

        assertThat(savedUser.getEScore()).isEqualTo(45);
        assertThat(savedUser.getSScore()).isEqualTo(225);
        assertThat(savedUser.getGActivityScore()).isEqualTo(90);
        assertThat(savedUser.getGRepaymentScore()).isEqualTo(100);
        assertThat(histories.stream().filter(history -> history.getReason() == ScoreReason.NO_ACTIVITY).count()).isEqualTo(3);
    }

    private void seedScoreFloor(User user) {
        validScoreHistoryRepository.saveAll(List.of(
                ValidScoreHistory.create(user, ScoreCategory.E, 50, ScoreReason.INITIAL_SCORE, null, 50),
                ValidScoreHistory.create(user, ScoreCategory.S, 250, ScoreReason.INITIAL_SCORE, null, 250),
                ValidScoreHistory.create(user, ScoreCategory.G_ACTIVITY, 100, ScoreReason.INITIAL_SCORE, null, 100),
                ValidScoreHistory.create(user, ScoreCategory.G_REPAYMENT, 100, ScoreReason.INITIAL_SCORE, null, 100),
                ValidScoreHistory.create(user, ScoreCategory.E, -5, ScoreReason.ABUSE, null, 45),
                ValidScoreHistory.create(user, ScoreCategory.S, -25, ScoreReason.ABUSE, null, 225),
                ValidScoreHistory.create(user, ScoreCategory.G_ACTIVITY, -10, ScoreReason.ABUSE, null, 90)
        ));
    }

    private void seedInitialScores(User user) {
        validScoreHistoryRepository.saveAll(List.of(
                ValidScoreHistory.create(user, ScoreCategory.E, 50, ScoreReason.INITIAL_SCORE, null, 50),
                ValidScoreHistory.create(user, ScoreCategory.S, 250, ScoreReason.INITIAL_SCORE, null, 250),
                ValidScoreHistory.create(user, ScoreCategory.G_ACTIVITY, 100, ScoreReason.INITIAL_SCORE, null, 100),
                ValidScoreHistory.create(user, ScoreCategory.G_REPAYMENT, 100, ScoreReason.INITIAL_SCORE, null, 100)
        ));
    }

    private void seedPolicies() {
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.E, 100, 5, 3, 10, 50));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.S, 500, 25, 3, 50, 250));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.G_ACTIVITY, 200, 10, 3, 20, 100));
        esgScorePolicyRepository.save(createEsgScorePolicy(ScoreCategory.G_REPAYMENT, 200, 0, 0, 0, 100));
    }

    private PenaltyPolicy createPenaltyPolicy(
            PenaltyType penaltyType,
            int eReduction,
            int sReduction,
            int gReduction,
            boolean blockLoan
    ) {
        PenaltyPolicy penaltyPolicy = newInstance(PenaltyPolicy.class);
        ReflectionTestUtils.setField(penaltyPolicy, "penaltyType", penaltyType);
        ReflectionTestUtils.setField(penaltyPolicy, "eReduction", eReduction);
        ReflectionTestUtils.setField(penaltyPolicy, "sReduction", sReduction);
        ReflectionTestUtils.setField(penaltyPolicy, "gReduction", gReduction);
        ReflectionTestUtils.setField(penaltyPolicy, "isBaseReduction", penaltyType == PenaltyType.ABUSE);
        ReflectionTestUtils.setField(penaltyPolicy, "isBlockLoan", blockLoan);
        return penaltyPolicy;
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

    private User createUser(String loginId, int eScore, int sScore, int gActivityScore, int gRepaymentScore) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "loginId", loginId);
        ReflectionTestUtils.setField(user, "password", "password");
        ReflectionTestUtils.setField(user, "name", "tester");
        ReflectionTestUtils.setField(user, "phoneNumber", "01012345678");
        ReflectionTestUtils.setField(user, "birthdate", LocalDate.of(2000, 1, 1));
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
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        return user;
    }

    private <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }
}
