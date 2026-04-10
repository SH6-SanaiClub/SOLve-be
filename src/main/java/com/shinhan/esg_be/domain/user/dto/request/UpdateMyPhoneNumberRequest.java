package com.shinhan.esg_be.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 휴대폰 번호 변경 시 본인인증 impUid를 받는 요청 DTO
public class UpdateMyPhoneNumberRequest {

    @NotBlank(message = "impUid는 필수입니다.")
    private String impUid;
}
