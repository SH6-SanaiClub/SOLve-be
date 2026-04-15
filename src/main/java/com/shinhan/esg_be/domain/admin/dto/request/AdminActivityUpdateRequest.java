package com.shinhan.esg_be.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminActivityUpdateRequest {

    @NotBlank(message = "활동명은 필수입니다.")
    private String name;
}
