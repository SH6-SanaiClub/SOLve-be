package com.shinhan.esg_be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 비밀번호 변경 완료 메시지를 내려주는 응답 DTO
public class UpdateMyPasswordResponse {

    private final String message;
}
