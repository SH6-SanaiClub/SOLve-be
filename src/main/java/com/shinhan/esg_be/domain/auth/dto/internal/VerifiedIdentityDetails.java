package com.shinhan.esg_be.domain.auth.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 본인인증 결과에서 번호 변경에 필요한 값을 담는 DTO
public class VerifiedIdentityDetails {

    private final String ciDi;
    private final String name;
    private final String phoneNumber;
    private final String birthdate;
}
