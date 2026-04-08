package com.shinhan.esg_be.domain.bank.controller;

import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductListResponse;
import com.shinhan.esg_be.domain.bank.dto.response.FinanceProductResponse;
import com.shinhan.esg_be.domain.bank.service.FinanceService;
import com.shinhan.esg_be.global.exception.GlobalExceptionHandler;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private FinanceController financeController;

    @Mock
    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(financeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("금융 상품 조회 API는 상품 목록을 반환한다")
    void getFinanceProducts() throws Exception {
        FinanceProductResponse response = new FinanceProductResponse(
                1L,
                "ESG 소액대출",
                "금융 이력이 부족해도 ESG 점수로 공정하게",
                "LOAN",
                new BigDecimal("8.50"),
                new BigDecimal("8.50"),
                new BigDecimal("7.00"),
                2_000_000L,
                true,
                12,
                null,
                "설명"
        );

        given(financeService.getFinanceProducts(isNull(), eq("loan")))
                .willReturn(new FinanceProductListResponse(List.of(response)));

        mockMvc.perform(get("/api/v1/finance/list")
                        .param("type", "loan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].id").value(1))
                .andExpect(jsonPath("$.products[0].name").value("ESG 소액대출"))
                .andExpect(jsonPath("$.products[0].subtitle").value("금융 이력이 부족해도 ESG 점수로 공정하게"))
                .andExpect(jsonPath("$.products[0].loanLimit").value(2000000))
                .andExpect(jsonPath("$.products[0].appliedRate").value(7.00));
    }
}
