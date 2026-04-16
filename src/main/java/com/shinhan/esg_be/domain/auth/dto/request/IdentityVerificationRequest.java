package com.shinhan.esg_be.domain.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdentityVerificationRequest {

    @NotBlank
    @JsonAlias("imp_uid")
    private String impUid;
}
