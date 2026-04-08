package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductListResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
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
    private UserLoanRepository userLoanRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("점수 800점 이상 사용자는 대출 상품 조회 시 200만원 한도와 7퍼센트 금리를 받는다")
    void getLoanProducts() {
        User user = createUser("finance-user-1", 100, 500, 100, 100);
        FinancialProduct product = createFinancialProduct("ESG 소액대출", ProductType.LOAN, "8.50", "8.50", 12);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.LOAN)).willReturn(List.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of());

        FinanceProductListResponse response = financeService.getFinanceProducts(user.getLoginId(), "loan");

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).subtitle()).isEqualTo("ESG 소액대출 subtitle");
        assertThat(response.products().get(0).available()).isTrue();
        assertThat(response.products().get(0).loanLimit()).isEqualTo(2_000_000L);
        assertThat(response.products().get(0).appliedRate()).isEqualByComparingTo("7.00");
        assertThat(response.products().get(0).monthlyPaymentAmount()).isNull();
    }

    @Test
    @DisplayName("활성 대출이 있으면 대출 상품은 조회되지만 가입 가능 상태는 false다")
    void getLoanProductsWithActiveLoan() {
        User user = createUser("finance-user-2", 100, 500, 200, 100);
        FinancialProduct product = createFinancialProduct("ESG 소액대출", ProductType.LOAN, "8.50", "8.50", 12);
        UserLoan activeLoan = createUserLoan(user, product);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.LOAN)).willReturn(List.of(product));
        given(userLoanRepository.findByUserAndStatus(user, LoanStatus.ACTIVE)).willReturn(List.of(activeLoan));

        FinanceProductListResponse response = financeService.getFinanceProducts(user.getLoginId(), "loan");

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).subtitle()).isEqualTo("ESG 소액대출 subtitle");
        assertThat(response.products().get(0).available()).isFalse();
        assertThat(response.products().get(0).loanLimit()).isNull();
        assertThat(response.products().get(0).appliedRate()).isNull();
        assertThat(response.products().get(0).monthlyPaymentAmount()).isNull();
    }

    @Test
    @DisplayName("적금 상품 조회 시 기본 금리와 최대 금리를 그대로 반환한다")
    void getSavingProducts() {
        User user = createUser("finance-user-3", 50, 250, 100, 100);
        FinancialProduct product = createFinancialProduct("그린 스텝업 적금", ProductType.SAVINGS, "2.00", "4.40", 12);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS)).willReturn(List.of(product));

        FinanceProductListResponse response = financeService.getFinanceProducts(user.getLoginId(), "savings");

        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).subtitle()).isEqualTo("그린 스텝업 적금 subtitle");
        assertThat(response.products().get(0).available()).isTrue();
        assertThat(response.products().get(0).baseRate()).isEqualByComparingTo("2.00");
        assertThat(response.products().get(0).maxRate()).isEqualByComparingTo("4.40");
        assertThat(response.products().get(0).appliedRate()).isEqualByComparingTo("2.00");
        assertThat(response.products().get(0).monthlyPaymentAmount()).isEqualTo(300_000L);
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
        ReflectionTestUtils.setField(product, "description", name + " 설명");
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
