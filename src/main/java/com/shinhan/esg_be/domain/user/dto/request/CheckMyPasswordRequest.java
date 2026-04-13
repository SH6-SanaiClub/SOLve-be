package com.shinhan.esg_be.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 현재 비밀번호 확인 시 비밀번호 값을 받는 요청 DTO
public class CheckMyPasswordRequest {

    @NotBlank(message = "currentPassword는 필수입니다.")
    private String currentPassword;
}
