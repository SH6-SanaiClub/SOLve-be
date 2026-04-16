package com.shinhan.esg_be.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminPenaltyRequest {

    @NotBlank(message = "패널티 사유는 필수입니다.")
    private String reason;
}
