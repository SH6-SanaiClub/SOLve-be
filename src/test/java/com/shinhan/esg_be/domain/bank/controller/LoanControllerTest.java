package com.shinhan.esg_be.domain.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.bank.dto.request.LoanApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.LoanApplyResponse;
import com.shinhan.esg_be.domain.bank.dto.response.LoanPreviewResponse;
import com.shinhan.esg_be.domain.bank.service.LoanService;
import com.shinhan.esg_be.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LoanController loanController;

    @Mock
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(loanController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("대출 미리보기 API는 적용 조건을 반환한다")
    void getLoanPreview() throws Exception {
        LoanPreviewResponse response = new LoanPreviewResponse(
                1L,
                "ESG 소액대출",
                "금융 이력이 부족해도 ESG 점수로 공정하게",
                "설명",
                true,
                "AVAILABLE",
                2_000_000L,
                new BigDecimal("7.00"),
                12,
                800,
                false,
                false
        );

        given(loanService.getLoanPreview(isNull(), eq(1L))).willReturn(response);

        mockMvc.perform(get("/api/v1/finance/loans/1/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.reason").value("AVAILABLE"))
                .andExpect(jsonPath("$.loanLimit").value(2000000))
                .andExpect(jsonPath("$.appliedRate").value(7.00));
    }

    @Test
    @DisplayName("대출 신청 API는 생성 상태를 반환한다")
    void applyLoan() throws Exception {
        LoanApplyRequest request = new LoanApplyRequest(1L, 1_500_000L);
        given(loanService.applyLoan(isNull(), any(LoanApplyRequest.class)))
                .willReturn(new LoanApplyResponse("ACTIVE"));

        mockMvc.perform(post("/api/v1/finance/loans/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
