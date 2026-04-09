package com.shinhan.esg_be.domain.point.entity;

import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_point")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_id")
    private Long exchangeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PointReason reason;

    @Column(name = "changed_amount", nullable = false)
    private Long changedAmount;

    @Column(name = "point_after", nullable = false)
    private Long pointAfter;

    @Column(name = "exchange_code", unique = true, length = 100)
    private String exchangeCode;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserPoint create(
            User user,
            Item item,
            PointReason reason,
            long changedAmount,
            long pointAfter
    ) {
        return create(user, item, reason, changedAmount, pointAfter, null);
    }

    public static UserPoint create(
            User user,
            Item item,
            PointReason reason,
            long changedAmount,
            long pointAfter,
            String exchangeCode
    ) {
        UserPoint userPoint = new UserPoint();
        userPoint.user = user;
        userPoint.item = item;
        userPoint.reason = reason;
        userPoint.changedAmount = changedAmount;
        userPoint.pointAfter = pointAfter;
        userPoint.exchangeCode = exchangeCode;
        return userPoint;
    }
}
