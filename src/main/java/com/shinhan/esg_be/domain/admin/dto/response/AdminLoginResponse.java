package com.shinhan.esg_be.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminLoginResponse {

    private String accessToken;
    private final String tokenType = "Bearer";
}
