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
public class AdminVolunteerUpdateRequest {

    @NotBlank(message = "봉사명은 필수입니다.")
    private String name;

    @NotBlank(message = "설명은 필수입니다.")
    private String description;

    @NotBlank(message = "장소는 필수입니다.")
    private String location;

    @NotNull(message = "정원은 필수입니다.")
    @Positive(message = "정원은 0보다 커야 합니다.")
    private Integer capacity;

    @NotNull(message = "활동일은 필수입니다.")
    private LocalDateTime activityDate;

    @NotNull(message = "봉사 시간은 필수입니다.")
    @Positive(message = "봉사 시간은 0보다 커야 합니다.")
    private Integer volunteerHour;

    @NotBlank(message = "기관명은 필수입니다.")
    private String organization;

    @NotBlank(message = "QR 토큰은 필수입니다.")
    private String qrToken;

    @NotNull(message = "위도는 필수입니다.")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다.")
    private Double longitude;
}
