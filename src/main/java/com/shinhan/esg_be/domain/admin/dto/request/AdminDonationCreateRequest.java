package com.shinhan.esg_be.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminDonationCreateRequest {

    @NotBlank(message = "기부명은 필수입니다.")
    private String name;

    @NotBlank(message = "요약은 필수입니다.")
    private String summary;

    @NotBlank(message = "설명은 필수입니다.")
    private String description;

    @NotNull(message = "목표 금액은 필수입니다.")
    @Positive(message = "목표 금액은 0보다 커야 합니다.")
    private Long targetAmount;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDateTime endDate;

    @NotBlank(message = "이미지 URL은 필수입니다.")
    private String imageUrl;

    @NotNull(message = "활성 상태는 필수입니다.")
    private Boolean isActive;
}
