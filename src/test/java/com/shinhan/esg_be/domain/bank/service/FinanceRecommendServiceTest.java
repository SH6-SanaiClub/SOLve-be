package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.response.SavingsRecommendResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.domain.policy.repository.EsgScorePolicyRepository;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.entity.UserScoreSnapshot;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.stat.repository.UserScoreSnapshotRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FinanceRecommendServiceTest {

    @Mock
    private FinancialProductRepository financialProductRepository;

    @Mock
    private UserSavingRepository userSavingRepository;

    @Mock
    private UserMonthlyStatRepository userMonthlyStatRepository;

    @Mock
    private UserScoreSnapshotRepository userScoreSnapshotRepository;

    @Mock
    private EsgScorePolicyRepository esgScorePolicyRepository;

    @Mock
    private UserRepository userRepository;

    private FinanceRecommendService financeRecommendService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-04-19T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        FinanceRecommendFeatureExtractor featureExtractor = new FinanceRecommendFeatureExtractor(
                userMonthlyStatRepository,
                userScoreSnapshotRepository,
                esgScorePolicyRepository,
                fixedClock
        );
        financeRecommendService = new FinanceRecommendService(
                financialProductRepository,
                userSavingRepository,
                userMonthlyStatRepository,
                userRepository,
                featureExtractor,
                new SavingsProductScorer()
        );
    }

    @Test
    @DisplayName("활동 이력이 전혀 없으면 그린 스텝업 적금을 시작 추천한다")
    void recommendStartProductForTrueNewUser() {
        User user = createUser("new-finance-user", 500);
        FinancialProduct greenStepUp = createProduct(1L, "그린 스텝업 적금", "2.00", "5.00");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(userSavingRepository.findAllByUser_UserIdAndStatus(user.getUserId(), SavingStatus.ACTIVE))
                .willReturn(List.of());
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS))
                .willReturn(List.of(greenStepUp));
        given(userMonthlyStatRepository.countByUser_UserId(user.getUserId())).willReturn(0L);

        SavingsRecommendResponse response = financeRecommendService.recommend(user.getLoginId());

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.getRecommendation()).isNotNull();
        assertThat(response.getRecommendation().getProductName()).isEqualTo("그린 스텝업 적금");
        assertThat(response.getRecommendation().getExpectedMaxRate()).isEqualTo("연 5%");
    }

    @Test
    @DisplayName("최근 3개월 E 활동이 강하면 지구 수호대 적금을 추천한다")
    void recommendEarthGuardianForRecentEPattern() {
        User user = createUser("earth-user", 640);
        FinancialProduct earthGuardian = createProduct(1L, "지구 수호대 적금", "3.00", "5.00");
        FinancialProduct warmCompanion = createProduct(2L, "따뜻한 동행 적금", "3.00", "7.00");
        FinancialProduct greenStepUp = createProduct(3L, "그린 스텝업 적금", "2.00", "5.00");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(userSavingRepository.findAllByUser_UserIdAndStatus(user.getUserId(), SavingStatus.ACTIVE))
                .willReturn(List.of());
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS))
                .willReturn(List.of(earthGuardian, warmCompanion, greenStepUp));
        given(userMonthlyStatRepository.countByUser_UserId(user.getUserId())).willReturn(3L);
        given(userMonthlyStatRepository.findTop3ByUser_UserIdOrderByCreatedAtDesc(user.getUserId()))
                .willReturn(List.of(
                        createMonthlyStat(user, LocalDateTime.of(2026, 4, 1, 0, 0), 5, 5, 2, 3, 0, 0),
                        createMonthlyStat(user, LocalDateTime.of(2026, 3, 1, 0, 0), 5, 4, 1, 2, 0, 0),
                        createMonthlyStat(user, LocalDateTime.of(2026, 2, 1, 0, 0), 4, 3, 0, 1, 0, 0)
                ));
        given(esgScorePolicyRepository.findAllByIsActiveTrue()).willReturn(defaultPolicies());
        given(userScoreSnapshotRepository.findByUser_UserIdOrderBySnapshotDateDesc(user.getUserId()))
                .willReturn(List.of(
                        createSnapshot(user, LocalDate.of(2026, 3, 31), 620),
                        createSnapshot(user, LocalDate.of(2026, 2, 28), 590)
                ));

        SavingsRecommendResponse response = financeRecommendService.recommend(user.getLoginId());

        assertThat(response.isNewUser()).isFalse();
        assertThat(response.getRecommendation()).isNotNull();
        assertThat(response.getRecommendation().getProductName()).isEqualTo("지구 수호대 적금");
        assertThat(response.getRecommendation().getReason()).contains("E");
    }

    @Test
    @DisplayName("최근 3개월 활동 데이터가 없으면 범용형 적금으로 저신뢰 fallback 한다")
    void recommendGreenStepUpWhenRecentDataIsMissing() {
        User user = createUser("stale-user", 700);
        FinancialProduct greenStepUp = createProduct(1L, "그린 스텝업 적금", "2.00", "5.00");
        FinancialProduct earthGuardian = createProduct(2L, "지구 수호대 적금", "3.00", "5.00");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(userSavingRepository.findAllByUser_UserIdAndStatus(user.getUserId(), SavingStatus.ACTIVE))
                .willReturn(List.of());
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS))
                .willReturn(List.of(greenStepUp, earthGuardian));
        given(userMonthlyStatRepository.countByUser_UserId(user.getUserId())).willReturn(2L);
        given(userMonthlyStatRepository.findTop3ByUser_UserIdOrderByCreatedAtDesc(user.getUserId()))
                .willReturn(List.of(
                        createMonthlyStat(user, LocalDateTime.of(2025, 12, 1, 0, 0), 5, 10, 3, 1, 0, 0),
                        createMonthlyStat(user, LocalDateTime.of(2025, 11, 1, 0, 0), 4, 8, 2, 0, 0, 0)
                ));
        given(esgScorePolicyRepository.findAllByIsActiveTrue()).willReturn(defaultPolicies());
        given(userScoreSnapshotRepository.findByUser_UserIdOrderBySnapshotDateDesc(user.getUserId()))
                .willReturn(List.of());

        SavingsRecommendResponse response = financeRecommendService.recommend(user.getLoginId());

        assertThat(response.isNewUser()).isFalse();
        assertThat(response.getRecommendation()).isNotNull();
        assertThat(response.getRecommendation().getProductName()).isEqualTo("그린 스텝업 적금");
        assertThat(response.getRecommendation().getReason()).contains("최근 3개월 활동 데이터가 부족");
    }

    @Test
    @DisplayName("ESG 마스터 적금 가입 점수가 부족하면 추천 후보에서 제외한다")
    void excludeIneligibleEsgMaster() {
        User user = createUser("master-low-user", 860);
        FinancialProduct esgMaster = createProduct(1L, "ESG 마스터 적금", "4.00", "10.00");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(userSavingRepository.findAllByUser_UserIdAndStatus(user.getUserId(), SavingStatus.ACTIVE))
                .willReturn(List.of());
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS))
                .willReturn(List.of(esgMaster));

        SavingsRecommendResponse response = financeRecommendService.recommend(user.getLoginId());

        assertThat(response.getRecommendation()).isNull();
    }

    private User createUser(String loginId, int totalScore) {
        User user = User.create(
                loginId,
                "password",
                "Finance User",
                loginId + "@example.com",
                "01012345678",
                LocalDate.of(2000, 1, 1),
                loginId + "-ci"
        );
        ReflectionTestUtils.setField(user, "userId", Math.abs((long) loginId.hashCode()));
        user.replaceScores(50, 250, 100, Math.max(totalScore - 400, 0));
        return user;
    }

    private FinancialProduct createProduct(Long id, String name, String baseRate, String maxRate) {
        FinancialProduct product = FinancialProduct.create(
                name,
                name + " subtitle",
                ProductType.SAVINGS,
                new BigDecimal(baseRate),
                new BigDecimal(maxRate),
                "desc",
                12,
                300_000L,
                true
        );
        ReflectionTestUtils.setField(product, "finProductId", id);
        return product;
    }

    private UserMonthlyStat createMonthlyStat(
            User user,
            LocalDateTime createdAt,
            int eScore,
            int sScore,
            int gScore,
            int consecutiveE,
            int consecutiveS,
            int consecutiveG
    ) {
        UserMonthlyStat stat = UserMonthlyStat.create(user);
        ReflectionTestUtils.setField(stat, "createdAt", createdAt);
        ReflectionTestUtils.setField(stat, "monthlyEScore", eScore);
        ReflectionTestUtils.setField(stat, "monthlySScore", sScore);
        ReflectionTestUtils.setField(stat, "monthlyGScore", gScore);
        ReflectionTestUtils.setField(stat, "consecutiveEMaxScore", consecutiveE);
        ReflectionTestUtils.setField(stat, "consecutiveSMaxScore", consecutiveS);
        ReflectionTestUtils.setField(stat, "consecutiveGMaxScore", consecutiveG);
        return stat;
    }

    private UserScoreSnapshot createSnapshot(User user, LocalDate snapshotDate, int totalScore) {
        UserScoreSnapshot snapshot = UserScoreSnapshot.create(user, snapshotDate);
        ReflectionTestUtils.setField(snapshot, "snapshotDate", snapshotDate);
        ReflectionTestUtils.setField(snapshot, "totalScore", totalScore);
        return snapshot;
    }

    private List<EsgScorePolicy> defaultPolicies() {
        return List.of(
                createPolicy(ScoreCategory.E, 5),
                createPolicy(ScoreCategory.S, 25),
                createPolicy(ScoreCategory.G_ACTIVITY, 10)
        );
    }

    private EsgScorePolicy createPolicy(ScoreCategory category, int monthlyMaxScore) {
        EsgScorePolicy policy = newInstance(EsgScorePolicy.class);
        ReflectionTestUtils.setField(policy, "category", category);
        ReflectionTestUtils.setField(policy, "monthlyMaxScore", monthlyMaxScore);
        ReflectionTestUtils.setField(policy, "isActive", true);
        return policy;
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
