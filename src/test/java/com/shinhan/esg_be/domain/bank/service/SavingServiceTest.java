package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.request.SavingApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.SavingApplyResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SavingServiceTest {

    @InjectMocks
    private SavingService savingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FinancialProductRepository financialProductRepository;

    @Mock
    private UserSavingRepository userSavingRepository;

    @Test
    @DisplayName("적금 가입 시 원장을 생성하고 ACTIVE 상태를 반환한다")
    void applySaving() {
        User user = createUser("saving-user-1", 100, 400, 200, 100);
        FinancialProduct product = createSavingProduct(2L, "그린 스텝업 적금", 300_000L, 12);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(2L, ProductType.SAVINGS))
                .willReturn(Optional.of(product));

        SavingApplyResponse response = savingService.applySaving(user.getLoginId(), new SavingApplyRequest(2L));

        ArgumentCaptor<UserSaving> captor = ArgumentCaptor.forClass(UserSaving.class);
        verify(userSavingRepository).save(captor.capture());
        UserSaving saved = captor.getValue();

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(saved.getMonthlyAmount()).isEqualTo(300_000L);
        assertThat(saved.getScore()).isEqualTo(800);
        assertThat(saved.getMaturityDate()).isEqualTo(LocalDate.now().plusMonths(12));
    }

    @Test
    @DisplayName("적금 상품이 아니면 가입을 거절한다")
    void rejectWhenSavingProductNotFound() {
        User user = createUser("saving-user-2", 50, 250, 100, 100);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(99L, ProductType.SAVINGS))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> savingService.applySaving(user.getLoginId(), new SavingApplyRequest(99L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("적금 상품을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("이미 가입한 적금 상품이면 가입을 거절한다")
    void rejectWhenSameSavingProductAlreadyActive() {
        User user = createUser("saving-user-3", 100, 400, 200, 100);
        FinancialProduct product = createSavingProduct(2L, "Green Saving", 300_000L, 12);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(2L, ProductType.SAVINGS))
                .willReturn(Optional.of(product));
        given(userSavingRepository.existsByUserAndFinancialProductAndStatus(user, product, SavingStatus.ACTIVE))
                .willReturn(true);

        assertThatThrownBy(() -> savingService.applySaving(user.getLoginId(), new SavingApplyRequest(2L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Already joined saving product.");
    }

    @Test
    @DisplayName("ESG 마스터 적금은 900점 미만이면 가입을 거절한다")
    void rejectEsgMasterWhenScoreIsTooLow() {
        User user = createUser("saving-user-master-low", 50, 250, 100, 100);
        FinancialProduct product = createSavingProduct(5L, "ESG 마스터 적금", 300_000L, 12);

        given(userRepository.findByLoginId(user.getLoginId())).willReturn(Optional.of(user));
        given(financialProductRepository.findByFinProductIdAndTypeAndIsActiveTrue(5L, ProductType.SAVINGS))
                .willReturn(Optional.of(product));

        assertThatThrownBy(() -> savingService.applySaving(user.getLoginId(), new SavingApplyRequest(5L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("ESG 마스터 적금은 900점 이상부터 가입할 수 있습니다.");
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

    private FinancialProduct createSavingProduct(Long id, String name, Long monthlyPaymentAmount, int durationMonths) {
        FinancialProduct product = newInstance(FinancialProduct.class);
        ReflectionTestUtils.setField(product, "finProductId", id);
        ReflectionTestUtils.setField(product, "name", name);
        ReflectionTestUtils.setField(product, "type", ProductType.SAVINGS);
        ReflectionTestUtils.setField(product, "baseRate", new BigDecimal("2.00"));
        ReflectionTestUtils.setField(product, "maxRate", new BigDecimal("4.40"));
        ReflectionTestUtils.setField(product, "isActive", true);
        ReflectionTestUtils.setField(product, "durationMonths", durationMonths);
        ReflectionTestUtils.setField(product, "monthlyPaymentAmount", monthlyPaymentAmount);
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
