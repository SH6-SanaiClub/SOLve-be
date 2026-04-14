package com.shinhan.esg_be.domain.bank.controller;

import com.shinhan.esg_be.domain.bank.dto.request.SavingApplyRequest;
import com.shinhan.esg_be.domain.bank.dto.response.SavingApplyResponse;
import com.shinhan.esg_be.domain.bank.service.SavingService;
import com.shinhan.esg_be.global.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance/savings")
@RequiredArgsConstructor
public class SavingController {

    private final SavingService savingService;
    private final AuthContext authContext;

    @PostMapping("/apply")
    public ResponseEntity<SavingApplyResponse> applySaving(
            @Valid @RequestBody SavingApplyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savingService.applySaving(authContext.currentLoginId(), request));
    }
}
