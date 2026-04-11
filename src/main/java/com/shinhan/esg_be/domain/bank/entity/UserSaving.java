package com.shinhan.esg_be.domain.bank.entity;

import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_saving")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSaving {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "saving_id")
    private Long savingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fin_product_id", nullable = false)
    private FinancialProduct financialProduct;

    @Column(name = "monthly_amount", nullable = false)
    private Long monthlyAmount = 300000L;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SavingStatus status = SavingStatus.ACTIVE;

    @Column(name = "has_penalty", nullable = false)
    private Boolean hasPenalty;

    @Column(name = "master_bonus_eligible", nullable = false)
    private Boolean masterBonusEligible;

    @Column(nullable = false)
    private Integer score;

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserSaving create(
            User user,
            FinancialProduct financialProduct,
            Long monthlyAmount,
            LocalDate maturityDate,
            Integer score
    ) {
        UserSaving userSaving = new UserSaving();
        userSaving.user = user;
        userSaving.financialProduct = financialProduct;
        userSaving.monthlyAmount = monthlyAmount;
        userSaving.maturityDate = maturityDate;
        userSaving.status = SavingStatus.ACTIVE;
        userSaving.hasPenalty = false;
        userSaving.masterBonusEligible = true;
        userSaving.score = score;
        return userSaving;
    }

    public void complete() {
        this.status = SavingStatus.COMPLETE;
    }

    public void revokeMasterBonus() {
        this.masterBonusEligible = false;
    }
}
