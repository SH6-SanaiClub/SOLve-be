package com.shinhan.esg_be.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IdentityVerificationResponse {

    private boolean verified;
    private String verificationToken;
    private String preservedLoginId;
}
