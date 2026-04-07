package com.shinhan.esg_be.domain.auth.dto.request;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthLoginRequest {
    private String loginId;
    private String password;
}
