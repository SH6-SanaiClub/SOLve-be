package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminUserListResponse {

    private Long userId;
    private String loginId;
    private String name;
    private String email;
    private String phoneNumber;
    private Integer totalScore;
    private Grade currentGrade;
    private UserType userType;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static AdminUserListResponse from(User user) {
        return new AdminUserListResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getTotalScore(),
                user.getCurrentGrade(),
                user.getUserType(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}
