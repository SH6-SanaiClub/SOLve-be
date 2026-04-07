package com.shinhan.esg_be.domain.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthJoinRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email
    private String email;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(
            regexp = "^010-[0-9]{4}-[0-9]{4}$",
            message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)"
    )
    private String phoneNumber;

    @NotNull(message = "생년월일은 필수입니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthdate;

    @NotBlank(message = "본인인증 토큰은 필수입니다.")
    private String verificationToken;
}
