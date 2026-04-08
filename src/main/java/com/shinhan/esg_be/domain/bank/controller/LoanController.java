package com.shinhan.esg_be.domain.bank.controller;

import com.shinhan.esg_be.domain.bank.dto.request.LoanApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.LoanApplyResponse;
import com.shinhan.esg_be.domain.bank.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    public ResponseEntity<LoanApplyResponse> applyLoan(
            @AuthenticationPrincipal String loginId,
            @Valid @RequestBody LoanApplyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.applyLoan(loginId, request));
    }
}
