package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.request.LoanApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.LoanApplyResponse;
import com.shinhan.esg_be.domain.bank.dto.response.LoanPreviewResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @InjectMocks
    private LoanService loanService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FinancialProductRepository financialProductRepository;

    @Mock
    private UserLoanRepository userLoanRepository;

    @Test
    @DisplayName("Loan preview returns user-specific limit and rate")
    void getLoanPreview() {
        User user = createUser("loan-user-preview", 100, 400, 200, 100, 0);
        FinancialProduct product = createFinancialProduct(1L, "ESG Loan");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(1L, ProductType.LOAN))
                .willReturn(Optional.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of());

        LoanPreviewResponse response = loanService.getLoanPreview(user.getLoginId(), 1L);

        assertThat(response.available()).isTrue();
        assertThat(response.reason()).isEqualTo("AVAILABLE");
        assertThat(response.loanLimit()).isEqualTo(2_000_000L);
        assertThat(response.appliedRate()).isEqualByComparingTo("7.00");
        assertThat(response.durationMonths()).isEqualTo(12);
        assertThat(response.baseScore()).isEqualTo(800);
        assertThat(response.name()).isEqualTo("ESG Loan");
        assertThat(response.subtitle()).isEqualTo("ESG Loan subtitle");
    }

    @Test
    @DisplayName("Loan application creates active loan with score-based limit and rate")
    void applyLoan() {
        User user = createUser("loan-user-1", 100, 400, 200, 100, 0);
        FinancialProduct product = createFinancialProduct(1L, "ESG Loan");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(1L, ProductType.LOAN))
                .willReturn(Optional.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of());

        LoanApplyResponse response = loanService.applyLoan(
                user.getLoginId(),
                new LoanApplyRequest(1L, 1_500_000L)
        );

        ArgumentCaptor<UserLoan> captor = ArgumentCaptor.forClass(UserLoan.class);
        verify(userLoanRepository).save(captor.capture());
        UserLoan savedLoan = captor.getValue();

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(savedLoan.getPrincipalAmount()).isEqualTo(1_500_000L);
        assertThat(savedLoan.getCurrentRate()).isEqualByComparingTo("7.00");
        assertThat(savedLoan.getTotalAmount()).isEqualTo(1_605_000L);
        assertThat(savedLoan.getBaseEsgScore()).isEqualTo(800);
    }

    @Test
    @DisplayName("Loan application rejects when active loan exists")
    void rejectLoanWhenActiveLoanExists() {
        User user = createUser("loan-user-2", 100, 500, 100, 100, 0);
        FinancialProduct product = createFinancialProduct(1L, "ESG Loan");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(1L, ProductType.LOAN))
                .willReturn(Optional.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of(newInstance(UserLoan.class)));

        assertThatThrownBy(() -> loanService.applyLoan(user.getLoginId(), new LoanApplyRequest(1L, 1_000_000L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Loan application rejects duplicate active same product")
    void rejectLoanWhenSameLoanProductAlreadyActive() {
        User user = createUser("loan-user-duplicate", 100, 400, 200, 100, 0);
        FinancialProduct product = createFinancialProduct(1L, "ESG Loan");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(1L, ProductType.LOAN))
                .willReturn(Optional.of(product));
        given(userLoanRepository.existsByUserAndFinancialProductAndStatus(user, product, LoanStatus.ACTIVE))
                .willReturn(true);

        assertThatThrownBy(() -> loanService.applyLoan(user.getLoginId(), new LoanApplyRequest(1L, 1_000_000L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Already joined loan product.");
    }

    @Test
    @DisplayName("Loan application rejects when score is too low")
    void rejectLoanWhenScoreIsTooLow() {
        User user = createUser("loan-user-3", 50, 250, 100, 100, 0);
        FinancialProduct product = createFinancialProduct(1L, "ESG Loan");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(1L, ProductType.LOAN))
                .willReturn(Optional.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of());

        assertThatThrownBy(() -> loanService.applyLoan(user.getLoginId(), new LoanApplyRequest(1L, 1_000_000L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Loan application rejects blocked user")
    void rejectLoanWhenBlocked() {
        User user = createUser("loan-user-4", 100, 500, 100, 100, 1);
        FinancialProduct product = createFinancialProduct(1L, "ESG Loan");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(1L, ProductType.LOAN))
                .willReturn(Optional.of(product));

        assertThatThrownBy(() -> loanService.applyLoan(user.getLoginId(), new LoanApplyRequest(1L, 1_000_000L)))
                .isInstanceOf(BadRequestException.class);
    }

    private User createUser(String loginId, int eScore, int sScore, int gActivityScore, int gRepaymentScore, int abuseCount) {
        User user = newInstance(User.class);
        ReflectionTestUtils.setField(user, "loginId", loginId);
        ReflectionTestUtils.setField(user, "eScore", eScore);
        ReflectionTestUtils.setField(user, "sScore", sScore);
        ReflectionTestUtils.setField(user, "gActivityScore", gActivityScore);
        ReflectionTestUtils.setField(user, "gRepaymentScore", gRepaymentScore);
        ReflectionTestUtils.setField(user, "abuseCount", abuseCount);
        return user;
    }

    private FinancialProduct createFinancialProduct(Long id, String name) {
        FinancialProduct product = newInstance(FinancialProduct.class);
        ReflectionTestUtils.setField(product, "finProductId", id);
        ReflectionTestUtils.setField(product, "name", name);
        ReflectionTestUtils.setField(product, "subtitle", name + " subtitle");
        ReflectionTestUtils.setField(product, "type", ProductType.LOAN);
        ReflectionTestUtils.setField(product, "baseRate", new BigDecimal("8.50"));
        ReflectionTestUtils.setField(product, "maxRate", new BigDecimal("8.50"));
        ReflectionTestUtils.setField(product, "description", name + " description");
        ReflectionTestUtils.setField(product, "isActive", true);
        ReflectionTestUtils.setField(product, "durationMonths", 12);
        return product;
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
