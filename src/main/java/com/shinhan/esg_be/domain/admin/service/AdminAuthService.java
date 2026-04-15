package com.shinhan.esg_be.domain.admin.service;

import com.shinhan.esg_be.domain.admin.dto.request.AdminLoginRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminLoginResponse;
import com.shinhan.esg_be.domain.admin.entity.Admin;
import com.shinhan.esg_be.domain.admin.repository.AdminRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import com.shinhan.esg_be.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "아이디 또는 비밀번호가 올바르지 않습니다.";

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BadRequestException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BadRequestException(INVALID_CREDENTIALS_MESSAGE);
        }

        return new AdminLoginResponse(
                jwtTokenProvider.createAdminAccessToken(admin.getAdminId(), admin.getLoginId())
        );
    }
}
