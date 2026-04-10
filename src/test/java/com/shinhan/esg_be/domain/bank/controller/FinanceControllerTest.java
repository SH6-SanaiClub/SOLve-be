package com.shinhan.esg_be.domain.bank.controller;

import com.shinhan.esg_be.domain.bank.dto.response.ActiveLoanResponse;
import com.shinhan.esg_be.domain.bank.dto.response.ActiveSavingResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceHistoryResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceMyResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductListResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductResponse;
import com.shinhan.esg_be.domain.bank.dto.response.LoanHistoryResponse;
import com.shinhan.esg_be.domain.bank.dto.response.SavingHistoryResponse;
import com.shinhan.esg_be.domain.bank.service.FinanceService;
import com.shinhan.esg_be.global.exception.GlobalExceptionHandler;
import com.shinhan.esg_be.global.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceControllerTest {

    private static final String LOGIN_ID = "test-user";

    private MockMvc mockMvc;

    @InjectMocks
    private FinanceController financeController;

    @Mock
    private FinanceService financeService;

    @Mock
    private AuthContext authContext;

    @BeforeEach
    void setUp() {
        given(authContext.currentLoginId()).willReturn(LOGIN_ID);
        mockMvc = MockMvcBuilders.standaloneSetup(financeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("마이페이지 금융상품 조회 API는 활성 대출/적금 목록을 반환한다")
    void getMyFinance() throws Exception {
        FinanceMyResponse response = new FinanceMyResponse(
                List.of(new ActiveLoanResponse(
                        10L,
                        1L,
                        "ESG Loan",
                        1_500_000L,
                        1_605_000L,
                        100_000L,
                        1_505_000L,
                        1L,
                        new BigDecimal("7.00"),
                        "ACTIVE",
                        12,
                        LocalDate.of(2026, 5, 1),
                        LocalDateTime.of(2026, 4, 1, 0, 0)
                )),
                List.of(new ActiveSavingResponse(
                        20L,
                        2L,
                        "Green Saving",
                        300_000L,
                        new BigDecimal("1.00"),
                        new BigDecimal("3.00"),
                        300_000L,
                        1L,
                        11L,
                        "ACTIVE",
                        12,
                        false,
                        LocalDate.of(2027, 4, 1),
                        LocalDateTime.of(2026, 4, 1, 0, 0)
                ))
        );

        given(financeService.getMyFinance(LOGIN_ID)).willReturn(response);

        mockMvc.perform(get("/api/v1/finance/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loans[0].loanId").value(10))
                .andExpect(jsonPath("$.loans[0].productName").value("ESG Loan"))
                .andExpect(jsonPath("$.loans[0].paidAmount").value(100000))
                .andExpect(jsonPath("$.loans[0].remainingAmount").value(1505000))
                .andExpect(jsonPath("$.loans[0].repaymentCount").value(1))
                .andExpect(jsonPath("$.savings[0].savingId").value(20))
                .andExpect(jsonPath("$.savings[0].productName").value("Green Saving"))
                .andExpect(jsonPath("$.savings[0].addedRate").value(1.00))
                .andExpect(jsonPath("$.savings[0].appliedRate").value(3.00))
                .andExpect(jsonPath("$.savings[0].paidAmount").value(300000))
                .andExpect(jsonPath("$.savings[0].paymentCount").value(1))
                .andExpect(jsonPath("$.savings[0].remainingCount").value(11));
    }

    @Test
    @DisplayName("금융 이력 조회 API는 대출 및 적금 이력을 반환한다")
    void getFinanceHistory() throws Exception {
        FinanceHistoryResponse response = new FinanceHistoryResponse(
                List.of(new LoanHistoryResponse(
                        1L,
                        10L,
                        1L,
                        "ESG Loan",
                        100_000L,
                        LocalDateTime.of(2026, 4, 10, 9, 0)
                )),
                List.of(new SavingHistoryResponse(
                        2L,
                        20L,
                        2L,
                        "Green Saving",
                        300_000L,
                        LocalDateTime.of(2026, 4, 10, 9, 0)
                ))
        );

        given(financeService.getFinanceHistory(LOGIN_ID)).willReturn(response);

        mockMvc.perform(get("/api/v1/finance/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loans[0].historyId").value(1))
                .andExpect(jsonPath("$.loans[0].productName").value("ESG Loan"))
                .andExpect(jsonPath("$.savings[0].historyId").value(2))
                .andExpect(jsonPath("$.savings[0].productName").value("Green Saving"));
    }

    @Test
    @DisplayName("금융상품 조회 API는 상품 목록을 반환한다")
    void getFinanceProducts() throws Exception {
        FinanceProductResponse response = new FinanceProductResponse(
                1L,
                "ESG Loan",
                "ESG based loan product",
                "LOAN",
                new BigDecimal("8.50"),
                new BigDecimal("8.50"),
                new BigDecimal("7.00"),
                2_000_000L,
                true,
                12,
                null,
                "description"
        );

        given(financeService.getFinanceProducts(eq(LOGIN_ID), eq("loan")))
                .willReturn(new FinanceProductListResponse(List.of(response)));

        mockMvc.perform(get("/api/v1/finance/list").param("type", "loan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].id").value(1))
                .andExpect(jsonPath("$.products[0].name").value("ESG Loan"))
                .andExpect(jsonPath("$.products[0].subtitle").value("ESG based loan product"))
                .andExpect(jsonPath("$.products[0].loanLimit").value(2000000))
                .andExpect(jsonPath("$.products[0].appliedRate").value(7.00));
    }
}
