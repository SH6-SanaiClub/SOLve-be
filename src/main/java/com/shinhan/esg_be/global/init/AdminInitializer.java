package com.shinhan.esg_be.global.init;

import com.shinhan.esg_be.domain.admin.entity.Admin;
import com.shinhan.esg_be.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${admin.initial-login-id}")
    private String initialLoginId;

    @Value("${admin.initial-password}")
    private String initialPassword;

    @Override
    public void run(String... args) {
        if (adminRepository.count() > 0) {
            return;
        }

        Admin admin = Admin.create(initialLoginId, passwordEncoder.encode(initialPassword));
        adminRepository.save(admin);
        log.info("Initial admin account created. loginId={}", initialLoginId);
    }
}
