package com.shinhan.esg_be.domain.environment.entity;

import com.shinhan.esg_be.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEnvironmentActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "act_id")
    private Long actId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private EnvironmentActivity activity;

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserEnvironmentActivity create(
            User user,
            EnvironmentActivity activity,
            boolean isApproved,
            String ocrText,
            String adminComment
    ) {
        UserEnvironmentActivity userEnvironmentActivity = new UserEnvironmentActivity();
        userEnvironmentActivity.user = user;
        userEnvironmentActivity.activity = activity;
        userEnvironmentActivity.isApproved = isApproved;
        userEnvironmentActivity.ocrText = ocrText;
        userEnvironmentActivity.adminComment = adminComment;
        return userEnvironmentActivity;
    }
}
