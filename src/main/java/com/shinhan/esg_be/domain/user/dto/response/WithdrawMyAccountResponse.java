package com.shinhan.esg_be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 회원탈퇴 완료 메시지 응답 DTO
public class WithdrawMyAccountResponse {

    private final String message;
}
