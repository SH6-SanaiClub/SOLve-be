package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.response.FinanceHistoryResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceMyResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductListResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.LoanHistory;
import com.shinhan.esg_be.domain.bank.entity.SavingHistory;
import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingHistoryType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.LoanHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.SavingHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.SavingPrimeHistoryRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @InjectMocks
    private FinanceService financeService;

    @Mock
    private FinancialProductRepository financialProductRepository;

    @Mock
    private LoanHistoryRepository loanHistoryRepository;

    @Mock
    private SavingHistoryRepository savingHistoryRepository;

    @Mock
    private SavingPrimeHistoryRepository savingPrimeHistoryRepository;

    @Mock
    private UserLoanRepository userLoanRepository;

    @Mock
    private UserSavingRepository userSavingRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void getMyFinance() {
        User user = createUser("finance-user-my", 100, 500, 100, 100);
        ReflectionTestUtils.setField(user, "userId", 1L);

        FinancialProduct loanProduct = createFinancialProduct("ESG Loan", ProductType.LOAN, "8.50", "8.50", 12);
        ReflectionTestUtils.setField(loanProduct, "finProductId", 11L);
        UserLoan userLoan = createUserLoan(user, loanProduct);
        ReflectionTestUtils.setField(userLoan, "loanId", 101L);

        FinancialProduct savingProduct = createFinancialProduct("Green Saving", ProductType.SAVINGS, "2.00", "4.40", 12);
        ReflectionTestUtils.setField(savingProduct, "finProductId", 22L);
        UserSaving userSaving = createUserSaving(user, savingProduct);
        ReflectionTestUtils.setField(userSaving, "savingId", 202L);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(userLoanRepository.findAllByUser_UserId(1L)).willReturn(List.of(userLoan));
        given(userSavingRepository.findAllByUser_UserId(1L)).willReturn(List.of(userSaving));
        given(loanHistoryRepository.sumRepaymentAmountByLoanId(101L)).willReturn(100_000L);
        given(loanHistoryRepository.countRepaymentsByLoanId(101L)).willReturn(1L);
        given(savingHistoryRepository.sumAmountBySavingIdAndType(202L, SavingHistoryType.PAYMENT))
                .willReturn(300_000L);
        given(savingHistoryRepository.countBySavingIdAndType(202L, SavingHistoryType.PAYMENT))
                .willReturn(1L);
        given(savingPrimeHistoryRepository.findTopByUserSaving_SavingIdOrderByAppliedAtDesc(202L))
                .willReturn(Optional.empty());

        FinanceMyResponse response = financeService.getMyFinance(user.getLoginId());

        assertThat(response.loans()).hasSize(1);
        assertThat(response.loans().get(0).loanId()).isEqualTo(101L);
        assertThat(response.loans().get(0).productId()).isEqualTo(11L);
        assertThat(response.loans().get(0).productName()).isEqualTo("ESG Loan");
        assertThat(response.loans().get(0).paidAmount()).isEqualTo(100_000L);
        assertThat(response.loans().get(0).remainingAmount()).isEqualTo(985_000L);
        assertThat(response.loans().get(0).repaymentCount()).isEqualTo(1L);

        assertThat(response.savings()).hasSize(1);
        assertThat(response.savings().get(0).savingId()).isEqualTo(202L);
        assertThat(response.savings().get(0).productId()).isEqualTo(22L);
        assertThat(response.savings().get(0).productName()).isEqualTo("Green Saving");
        assertThat(response.savings().get(0).addedRate()).isEqualByComparingTo("0.00");
        assertThat(response.savings().get(0).appliedRate()).isEqualByComparingTo("2.00");
        assertThat(response.savings().get(0).paidAmount()).isEqualTo(300_000L);
        assertThat(response.savings().get(0).paymentCount()).isEqualTo(1L);
        assertThat(response.savings().get(0).remainingCount()).isEqualTo(11L);
        assertThat(response.savings().get(0).masterBonusEligible()).isTrue();
    }

    @Test
    void getMyFinanceIncludesCompletedProducts() {
        User user = createUser("finance-user-complete-my", 100, 500, 100, 100);
        ReflectionTestUtils.setField(user, "userId", 1L);

        FinancialProduct loanProduct = createFinancialProduct("Completed Loan", ProductType.LOAN, "8.50", "8.50", 12);
        ReflectionTestUtils.setField(loanProduct, "finProductId", 11L);
        UserLoan completedLoan = createUserLoan(user, loanProduct);
        ReflectionTestUtils.setField(completedLoan, "loanId", 101L);
        ReflectionTestUtils.setField(completedLoan, "status", LoanStatus.COMPLETE);

        FinancialProduct savingProduct = createFinancialProduct("Completed Saving", ProductType.SAVINGS, "2.00", "4.40", 12);
        ReflectionTestUtils.setField(savingProduct, "finProductId", 22L);
        UserSaving completedSaving = createUserSaving(user, savingProduct);
        ReflectionTestUtils.setField(completedSaving, "savingId", 202L);
        ReflectionTestUtils.setField(completedSaving, "status", SavingStatus.COMPLETE);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(userLoanRepository.findAllByUser_UserId(1L)).willReturn(List.of(completedLoan));
        given(userSavingRepository.findAllByUser_UserId(1L)).willReturn(List.of(completedSaving));
        given(loanHistoryRepository.sumRepaymentAmountByLoanId(101L)).willReturn(1_085_000L);
        given(loanHistoryRepository.countRepaymentsByLoanId(101L)).willReturn(12L);
        given(savingHistoryRepository.sumAmountBySavingIdAndType(202L, SavingHistoryType.PAYMENT))
                .willReturn(3_600_000L);
        given(savingHistoryRepository.countBySavingIdAndType(202L, SavingHistoryType.PAYMENT))
                .willReturn(12L);
        given(savingPrimeHistoryRepository.findTopByUserSaving_SavingIdOrderByAppliedAtDesc(202L))
                .willReturn(Optional.empty());

        FinanceMyResponse response = financeService.getMyFinance(user.getLoginId());

        assertThat(response.loans()).hasSize(1);
        assertThat(response.loans().get(0).status()).isEqualTo("COMPLETE");
        assertThat(response.savings()).hasSize(1);
        assertThat(response.savings().get(0).status()).isEqualTo("COMPLETE");
        assertThat(response.savings().get(0).remainingCount()).isZero();
    }

    @Test
    void getFinanceHistory() {
        User user = createUser("finance-user-history", 100, 500, 100, 100);
        ReflectionTestUtils.setField(user, "userId", 1L);

        FinancialProduct loanProduct = createFinancialProduct("ESG Loan", ProductType.LOAN, "8.50", "8.50", 12);
        ReflectionTestUtils.setField(loanProduct, "finProductId", 11L);
        UserLoan userLoan = createUserLoan(user, loanProduct);
        ReflectionTestUtils.setField(userLoan, "loanId", 101L);

        FinancialProduct savingProduct = createFinancialProduct("Green Saving", ProductType.SAVINGS, "2.00", "4.40", 12);
        ReflectionTestUtils.setField(savingProduct, "finProductId", 22L);
        UserSaving userSaving = createUserSaving(user, savingProduct);
        ReflectionTestUtils.setField(userSaving, "savingId", 202L);

        LoanHistory loanHistory = createLoanHistory(userLoan, 1L, 100_000L, LocalDateTime.of(2026, 4, 10, 9, 0));
        SavingHistory savingHistory = createSavingHistory(userSaving, 2L, 300_000L, LocalDateTime.of(2026, 4, 11, 9, 0));

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(loanHistoryRepository.findAllByUserId(1L)).willReturn(List.of(loanHistory));
        given(savingHistoryRepository.findAllByUserId(1L)).willReturn(List.of(savingHistory));

        FinanceHistoryResponse response = financeService.getFinanceHistory(user.getLoginId());

        assertThat(response.loans()).hasSize(1);
        assertThat(response.loans().get(0).historyId()).isEqualTo(1L);
        assertThat(response.loans().get(0).productName()).isEqualTo("ESG Loan");
        assertThat(response.savings()).hasSize(1);
        assertThat(response.savings().get(0).historyId()).isEqualTo(2L);
        assertThat(response.savings().get(0).productName()).isEqualTo("Green Saving");
    }

    @Test
    void getLoanProducts() {
        User user = createUser("finance-user-1", 100, 500, 100, 100);
        FinancialProduct product = createFinancialProduct("ESG Loan", ProductType.LOAN, "8.50", "8.50", 12);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.LOAN)).willReturn(List.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of());

        FinanceProductListResponse response = financeService.getFinanceProducts(user.getLoginId(), "loan");

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).subtitle()).isEqualTo("ESG Loan subtitle");
        assertThat(response.products().get(0).available()).isTrue();
        assertThat(response.products().get(0).unavailableReason()).isEqualTo("AVAILABLE");
        assertThat(response.products().get(0).loanLimit()).isEqualTo(2_000_000L);
        assertThat(response.products().get(0).appliedRate()).isEqualByComparingTo("7.00");
        assertThat(response.products().get(0).monthlyPaymentAmount()).isNull();
    }

    @Test
    void getLoanProductsWithActiveLoan() {
        User user = createUser("finance-user-2", 100, 500, 200, 100);
        FinancialProduct product = createFinancialProduct("ESG Loan", ProductType.LOAN, "8.50", "8.50", 12);
        UserLoan activeLoan = createUserLoan(user, product);
        ReflectionTestUtils.setField(activeLoan, "loanId", 301L);

        ReflectionTestUtils.setField(user, "userId", 1L);
        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.LOAN)).willReturn(List.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of(activeLoan));

        FinanceProductListResponse response = financeService.getFinanceProducts(user.getLoginId(), "loan");

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).subtitle()).isEqualTo("ESG Loan subtitle");
        assertThat(response.products().get(0).available()).isFalse();
        assertThat(response.products().get(0).unavailableReason()).isEqualTo("HAS_ACTIVE_LOAN");
        assertThat(response.products().get(0).loanLimit()).isNull();
        assertThat(response.products().get(0).appliedRate()).isNull();
        assertThat(response.products().get(0).monthlyPaymentAmount()).isNull();
    }

    @Test
    void getSavingProducts() {
        User user = createUser("finance-user-3", 50, 250, 100, 100);
        FinancialProduct product = createFinancialProduct("Green Saving", ProductType.SAVINGS, "2.00", "4.40", 12);

        ReflectionTestUtils.setField(user, "userId", 1L);
        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS)).willReturn(List.of(product));
        given(userSavingRepository.findAllByUser_UserIdAndStatus(1L, SavingStatus.ACTIVE)).willReturn(List.of());

        FinanceProductListResponse response = financeService.getFinanceProducts(user.getLoginId(), "savings");

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).subtitle()).isEqualTo("Green Saving subtitle");
        assertThat(response.products().get(0).available()).isTrue();
        assertThat(response.products().get(0).unavailableReason()).isEqualTo("AVAILABLE");
        assertThat(response.products().get(0).baseRate()).isEqualByComparingTo("2.00");
        assertThat(response.products().get(0).maxRate()).isEqualByComparingTo("4.40");
        assertThat(response.products().get(0).appliedRate()).isEqualByComparingTo("2.00");
        assertThat(response.products().get(0).monthlyPaymentAmount()).isEqualTo(300_000L);
    }

    @Test
    void getSavingProductsIncludesActiveSavingProductAsUnavailable() {
        User user = createUser("finance-user-4", 50, 250, 100, 100);
        ReflectionTestUtils.setField(user, "userId", 1L);

        FinancialProduct joinedProduct = createFinancialProduct("Joined Saving", ProductType.SAVINGS, "2.00", "4.40", 12);
        ReflectionTestUtils.setField(joinedProduct, "finProductId", 10L);
        FinancialProduct availableProduct = createFinancialProduct("Available Saving", ProductType.SAVINGS, "3.00", "5.40", 12);
        ReflectionTestUtils.setField(availableProduct, "finProductId", 20L);
        UserSaving activeSaving = createUserSaving(user, joinedProduct);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS))
                .willReturn(List.of(joinedProduct, availableProduct));
        given(userSavingRepository.findAllByUser_UserIdAndStatus(1L, SavingStatus.ACTIVE))
                .willReturn(List.of(activeSaving));

        FinanceProductListResponse response = financeService.getFinanceProducts(user.getLoginId(), "savings");

        assertThat(response.products()).hasSize(2);
        assertThat(response.products().get(0).id()).isEqualTo(10L);
        assertThat(response.products().get(0).name()).isEqualTo("Joined Saving");
        assertThat(response.products().get(0).available()).isFalse();
        assertThat(response.products().get(0).unavailableReason()).isEqualTo("ALREADY_JOINED");
        assertThat(response.products().get(1).id()).isEqualTo(20L);
        assertThat(response.products().get(1).name()).isEqualTo("Available Saving");
        assertThat(response.products().get(1).available()).isTrue();
        assertThat(response.products().get(1).unavailableReason()).isEqualTo("AVAILABLE");
    }

    @Test
    void getSavingProductsMarksEsgMasterUnavailableWhenScoreIsTooLow() {
        User user = createUser("finance-user-master-low", 50, 250, 100, 100);
        ReflectionTestUtils.setField(user, "userId", 1L);

        FinancialProduct product = createFinancialProduct("ESG 마스터 적금", ProductType.SAVINGS, "4.00", "10.00", 12);
        ReflectionTestUtils.setField(product, "finProductId", 30L);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS)).willReturn(List.of(product));
        given(userSavingRepository.findAllByUser_UserIdAndStatus(1L, SavingStatus.ACTIVE)).willReturn(List.of());

        FinanceProductListResponse response = financeService.getFinanceProducts(user.getLoginId(), "savings");

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).id()).isEqualTo(30L);
        assertThat(response.products().get(0).available()).isFalse();
        assertThat(response.products().get(0).unavailableReason()).isEqualTo("LOW_SCORE_FOR_ESG_MASTER");
    }

    @Test
    void getSavingProductsIncludesCompletedSavingProduct() {
        User user = createUser("finance-user-completed-saving", 50, 250, 100, 100);
        ReflectionTestUtils.setField(user, "userId", 1L);

        FinancialProduct completedProduct = createFinancialProduct("Completed Saving", ProductType.SAVINGS, "2.00", "4.40", 12);
        ReflectionTestUtils.setField(completedProduct, "finProductId", 10L);
        UserSaving completedSaving = createUserSaving(user, completedProduct);
        ReflectionTestUtils.setField(completedSaving, "status", SavingStatus.COMPLETE);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS))
                .willReturn(List.of(completedProduct));
        given(userSavingRepository.findAllByUser_UserIdAndStatus(1L, SavingStatus.ACTIVE))
                .willReturn(List.of());

        FinanceProductListResponse response = financeService.getFinanceProducts(user.getLoginId(), "savings");

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).id()).isEqualTo(10L);
        assertThat(response.products().get(0).name()).isEqualTo("Completed Saving");
        assertThat(response.products().get(0).available()).isTrue();
        assertThat(response.products().get(0).unavailableReason()).isEqualTo("AVAILABLE");
    }

    @Test
    void getSavingHistory() {
        User user = createUser("finance-user-saving-history", 100, 500, 100, 100);
        ReflectionTestUtils.setField(user, "userId", 1L);

        FinancialProduct savingProduct = createFinancialProduct("Green Saving", ProductType.SAVINGS, "2.00", "4.40", 12);
        ReflectionTestUtils.setField(savingProduct, "finProductId", 22L);
        UserSaving userSaving = createUserSaving(user, savingProduct);
        ReflectionTestUtils.setField(userSaving, "savingId", 202L);
        SavingHistory savingHistory = createSavingHistory(userSaving, 2L, 300_000L, LocalDateTime.of(2026, 4, 11, 9, 0));

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(userSavingRepository.findAllByUser_UserId(1L))
                .willReturn(List.of(userSaving));
        given(savingHistoryRepository.findAllByUserIdAndSavingId(1L, 202L))
                .willReturn(List.of(savingHistory));

        List<com.shinhan.esg_be.domain.bank.dto.response.SavingHistoryResponse> response =
                financeService.getSavingHistory(user.getLoginId(), 202L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).savingId()).isEqualTo(202L);
        assertThat(response.get(0).amount()).isEqualTo(300_000L);
    }

    @Test
    void rejectSavingHistoryWhenSavingNotOwned() {
        User user = createUser("finance-user-saving-history-denied", 100, 500, 100, 100);
        ReflectionTestUtils.setField(user, "userId", 1L);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(userSavingRepository.findAllByUser_UserId(1L))
                .willReturn(List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> financeService.getSavingHistory(user.getLoginId(), 999L))
                .isInstanceOf(com.shinhan.esg_be.global.exception.BadRequestException.class)
                .hasMessage("Saving product not found.");
    }

    private User createUser(String loginId, int eScore, int sScore, int gActivityScore, int gRepaymentScore) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "loginId", loginId);
        ReflectionTestUtils.setField(user, "eScore", eScore);
        ReflectionTestUtils.setField(user, "sScore", sScore);
        ReflectionTestUtils.setField(user, "gActivityScore", gActivityScore);
        ReflectionTestUtils.setField(user, "gRepaymentScore", gRepaymentScore);
        ReflectionTestUtils.setField(user, "abuseCount", 0);
        return user;
    }

    private FinancialProduct createFinancialProduct(
            String name,
            ProductType type,
            String baseRate,
            String maxRate,
            int durationMonths
    ) {
        FinancialProduct product = newInstance(FinancialProduct.class);
        ReflectionTestUtils.setField(product, "finProductId", 1L);
        ReflectionTestUtils.setField(product, "name", name);
        ReflectionTestUtils.setField(product, "subtitle", name + " subtitle");
        ReflectionTestUtils.setField(product, "type", type);
        ReflectionTestUtils.setField(product, "baseRate", new BigDecimal(baseRate));
        ReflectionTestUtils.setField(product, "maxRate", new BigDecimal(maxRate));
        ReflectionTestUtils.setField(product, "description", name + " description");
        ReflectionTestUtils.setField(product, "isActive", true);
        ReflectionTestUtils.setField(product, "durationMonths", durationMonths);
        ReflectionTestUtils.setField(product, "monthlyPaymentAmount", type == ProductType.SAVINGS ? 300_000L : null);
        return product;
    }

    private UserLoan createUserLoan(User user, FinancialProduct product) {
        UserLoan userLoan = newInstance(UserLoan.class);
        ReflectionTestUtils.setField(userLoan, "user", user);
        ReflectionTestUtils.setField(userLoan, "financialProduct", product);
        ReflectionTestUtils.setField(userLoan, "principalAmount", 1_000_000L);
        ReflectionTestUtils.setField(userLoan, "currentRate", new BigDecimal("8.50"));
        ReflectionTestUtils.setField(userLoan, "status", LoanStatus.ACTIVE);
        ReflectionTestUtils.setField(userLoan, "totalAmount", 1_085_000L);
        ReflectionTestUtils.setField(userLoan, "nextRepaymentDate", LocalDate.of(2026, 5, 1));
        ReflectionTestUtils.setField(userLoan, "baseEsgScore", user.getTotalScore());
        ReflectionTestUtils.setField(userLoan, "createdAt", LocalDateTime.of(2026, 4, 1, 0, 0));
        return userLoan;
    }

    private UserSaving createUserSaving(User user, FinancialProduct product) {
        UserSaving userSaving = newInstance(UserSaving.class);
        ReflectionTestUtils.setField(userSaving, "user", user);
        ReflectionTestUtils.setField(userSaving, "financialProduct", product);
        ReflectionTestUtils.setField(userSaving, "monthlyAmount", 300_000L);
        ReflectionTestUtils.setField(userSaving, "status", SavingStatus.ACTIVE);
        ReflectionTestUtils.setField(userSaving, "hasPenalty", false);
        ReflectionTestUtils.setField(userSaving, "masterBonusEligible", true);
        ReflectionTestUtils.setField(userSaving, "score", user.getTotalScore());
        ReflectionTestUtils.setField(userSaving, "maturityDate", LocalDate.of(2027, 4, 1));
        ReflectionTestUtils.setField(userSaving, "joinedAt", LocalDateTime.of(2026, 4, 1, 0, 0));
        return userSaving;
    }

    private LoanHistory createLoanHistory(UserLoan userLoan, Long id, Long amount, LocalDateTime paymentDate) {
        LoanHistory loanHistory = newInstance(LoanHistory.class);
        ReflectionTestUtils.setField(loanHistory, "id", id);
        ReflectionTestUtils.setField(loanHistory, "userLoan", userLoan);
        ReflectionTestUtils.setField(loanHistory, "amount", amount);
        ReflectionTestUtils.setField(loanHistory, "paymentDate", paymentDate);
        return loanHistory;
    }

    private SavingHistory createSavingHistory(UserSaving userSaving, Long id, Long amount, LocalDateTime paymentDate) {
        SavingHistory savingHistory = newInstance(SavingHistory.class);
        ReflectionTestUtils.setField(savingHistory, "id", id);
        ReflectionTestUtils.setField(savingHistory, "userSaving", userSaving);
        ReflectionTestUtils.setField(savingHistory, "amount", amount);
        ReflectionTestUtils.setField(savingHistory, "type", SavingHistoryType.PAYMENT);
        ReflectionTestUtils.setField(savingHistory, "paymentDate", paymentDate);
        return savingHistory;
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
