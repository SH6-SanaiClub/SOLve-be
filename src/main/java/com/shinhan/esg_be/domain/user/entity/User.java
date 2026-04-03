package com.shinhan.esg_be.domain.user.entity;

import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import com.shinhan.esg_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDateTime birthdate;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "ci_di", unique = true, nullable = false)
    private String ciDi;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType = UserType.ALL_ROUNDER;

    @Column(name = "abuse_count", nullable = false)
    private Integer abuseCount = 0;

    @Column(name = "e_score", nullable = false)
    private Integer eScore = 50;

    @Column(name = "s_score", nullable = false)
    private Integer sScore = 250;

    @Column(name = "g_activity_score", nullable = false)
    private Integer gActivityScore = 100;

    @Column(name = "g_repayment_score", nullable = false)
    private Integer gRepaymentScore = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_grade", nullable = false, length = 20)
    private Grade currentGrade = Grade.SEED;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "last_activity_date")
    private LocalDateTime lastActivityDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_linked", nullable = false)
    private Boolean isLinked = false;
    public static User create(String loginId, String password, String name, String email,
                              String phoneNumber, LocalDateTime birthdate, String ciDi) {
        User user = new User();
        user.loginId = loginId;
        user.password = password;
        user.name = name;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.birthdate = birthdate;
        user.ciDi = ciDi;

        // 초기 점수 설정 (총 500점)
        user.eScore = 50;
        user.sScore = 250;
        user.gActivityScore = 100;
        user.gRepaymentScore = 100;

        // 기본값 세팅
        user.userType = UserType.ALL_ROUNDER;
        user.currentGrade = Grade.SEED;
        user.abuseCount = 0;
        user.totalPoints = 0;
        user.isActive = true;
        user.isLinked = false;

        return user;
    }
}

