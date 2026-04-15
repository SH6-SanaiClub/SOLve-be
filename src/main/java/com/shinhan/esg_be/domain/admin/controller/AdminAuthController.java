package com.shinhan.esg_be.domain.admin.controller;

import com.shinhan.esg_be.domain.admin.dto.request.AdminLoginRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminLoginResponse;
import com.shinhan.esg_be.domain.admin.service.AdminAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody @Valid AdminLoginRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }
}
