package com.shinhan.esg_be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 이메일 변경 완료 후 내려주는 응답 DTO
public class UpdateMyEmailResponse {

    private final String email;
    private final String message;
}
