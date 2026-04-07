package com.shinhan.esg_be.domain.user.dto.response;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserMeResponse {

    private String loginId;
    private String name;
    private UserType userType;
    private Grade currentGrade;
    private String message;

    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getLoginId(),
                user.getName(),
                user.getUserType(),
                user.getCurrentGrade(),
                "인증된 사용자 정보를 조회했습니다."
        );
    }
}
