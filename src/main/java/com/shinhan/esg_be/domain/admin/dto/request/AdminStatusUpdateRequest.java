package com.shinhan.esg_be.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminStatusUpdateRequest {

    @NotNull(message = "활성 상태는 필수입니다.")
    private Boolean isActive;
}
