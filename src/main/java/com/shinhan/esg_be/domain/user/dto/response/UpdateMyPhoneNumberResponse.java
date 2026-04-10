package com.shinhan.esg_be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 휴대폰 번호 변경 완료 후 내려줄 응답 DTO
public class UpdateMyPhoneNumberResponse {

    private final String phoneNumber;
    private final String message;
}
