package com.shinhan.esg_be.domain.environment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class EnvironmentVerificationCreateRequest {

    @NotBlank
    private String activityType;

    @NotNull
    private MultipartFile image;
}
