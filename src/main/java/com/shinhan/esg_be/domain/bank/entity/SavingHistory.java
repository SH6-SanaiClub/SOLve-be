package com.shinhan.esg_be.domain.bank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "saving_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saving_id")
    private UserSaving userSaving;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    public static SavingHistory create(
            UserSaving userSaving,
            Long amount,
            LocalDateTime paymentDate
    ) {
        SavingHistory savingHistory = new SavingHistory();
        savingHistory.userSaving = userSaving;
        savingHistory.amount = amount;
        savingHistory.paymentDate = paymentDate;
        return savingHistory;
    }
}
