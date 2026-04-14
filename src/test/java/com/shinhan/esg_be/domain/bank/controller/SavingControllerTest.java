package com.shinhan.esg_be.domain.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.bank.dto.request.SavingApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.SavingApplyResponse;
import com.shinhan.esg_be.domain.bank.service.SavingService;
import com.shinhan.esg_be.global.exception.GlobalExceptionHandler;
import com.shinhan.esg_be.global.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SavingControllerTest {

    private static final String LOGIN_ID = "test-user";

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SavingController savingController;

    @Mock
    private SavingService savingService;

    @Mock
    private AuthContext authContext;

    @BeforeEach
    void setUp() {
        given(authContext.currentLoginId()).willReturn(LOGIN_ID);
        mockMvc = MockMvcBuilders.standaloneSetup(savingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("적금 가입 API는 생성 상태를 반환한다")
    void applySaving() throws Exception {
        SavingApplyRequest request = new SavingApplyRequest(2L);
        given(savingService.applySaving(eq(LOGIN_ID), any(SavingApplyRequest.class)))
                .willReturn(new SavingApplyResponse("ACTIVE"));

        mockMvc.perform(post("/api/v1/finance/savings/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
