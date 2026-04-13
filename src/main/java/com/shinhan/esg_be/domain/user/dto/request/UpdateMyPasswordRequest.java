package com.shinhan.esg_be.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 비밀번호 변경 시 현재/새 비밀번호를 받는 요청 DTO
public class UpdateMyPasswordRequest {

    @NotBlank(message = "currentPassword는 필수입니다.")
    private String currentPassword;

    @NotBlank(message = "newPassword는 필수입니다.")
    private String newPassword;

    @NotBlank(message = "newPasswordConfirm는 필수입니다.")
    private String newPasswordConfirm;
}
