package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.request.LoanApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.LoanApplyResponse;
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
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("대출 신청 시 점수 구간에 맞는 한도와 금리로 원장을 생성한다")
    void applyLoan() {
        User user = createUser("loan-user-1", 100, 400, 200, 100, 0);
        FinancialProduct product = createFinancialProduct(1L, "ESG 소액대출");

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
    @DisplayName("활성 대출이 있으면 대출 신청을 거절한다")
    void rejectLoanWhenActiveLoanExists() {
        User user = createUser("loan-user-2", 100, 500, 100, 100, 0);
        FinancialProduct product = createFinancialProduct(1L, "ESG 소액대출");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(1L, ProductType.LOAN))
                .willReturn(Optional.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of(newInstance(UserLoan.class)));

        assertThatThrownBy(() -> loanService.applyLoan(user.getLoginId(), new LoanApplyRequest(1L, 1_000_000L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("기존 대출 상환 전까지 추가 대출이 불가능합니다.");
    }

    @Test
    @DisplayName("대출 가능 점수를 충족하지 못하면 대출 신청을 거절한다")
    void rejectLoanWhenScoreIsTooLow() {
        User user = createUser("loan-user-3", 50, 250, 100, 100, 0);
        FinancialProduct product = createFinancialProduct(1L, "ESG 소액대출");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(1L, ProductType.LOAN))
                .willReturn(Optional.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of());

        assertThatThrownBy(() -> loanService.applyLoan(user.getLoginId(), new LoanApplyRequest(1L, 1_000_000L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("대출 신청 가능 점수를 충족하지 않습니다.");
    }

    @Test
    @DisplayName("대출 차단 상태면 대출 신청을 거절한다")
    void rejectLoanWhenBlocked() {
        User user = createUser("loan-user-4", 100, 500, 100, 100, 1);
        FinancialProduct product = createFinancialProduct(1L, "ESG 소액대출");

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(1L, ProductType.LOAN))
                .willReturn(Optional.of(product));

        assertThatThrownBy(() -> loanService.applyLoan(user.getLoginId(), new LoanApplyRequest(1L, 1_000_000L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("대출이 제한된 사용자입니다.");
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
        ReflectionTestUtils.setField(product, "type", ProductType.LOAN);
        ReflectionTestUtils.setField(product, "baseRate", new BigDecimal("8.50"));
        ReflectionTestUtils.setField(product, "maxRate", new BigDecimal("8.50"));
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
