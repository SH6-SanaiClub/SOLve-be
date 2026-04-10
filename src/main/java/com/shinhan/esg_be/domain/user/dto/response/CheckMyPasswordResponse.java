package com.shinhan.esg_be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 현재 비밀번호 일치 여부를 내려주는 응답 DTO
public class CheckMyPasswordResponse {

    private final boolean matched;
    private final String message;
}
