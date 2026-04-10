package com.shinhan.esg_be.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shinhan.esg_be.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
// 마이페이지 프로필 화면에 내려줄 응답 DTO
public class MyProfileResponse {

    private final String loginId;
    private final String name;
    private final String email;
    private final String phoneNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate birthdate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime joinedAt;

    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getBirthdate(),
                user.getCreatedAt()
        );
    }
}
