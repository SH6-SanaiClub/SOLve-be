package com.shinhan.esg_be.domain.bank.entity;

import com.shinhan.esg_be.domain.bank.entity.enums.SavingHistoryType;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SavingHistoryType type;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    public static SavingHistory create(
            UserSaving userSaving,
            Long amount,
            SavingHistoryType type,
            LocalDateTime paymentDate
    ) {
        SavingHistory savingHistory = new SavingHistory();
        savingHistory.userSaving = userSaving;
        savingHistory.amount = amount;
        savingHistory.type = type;
        savingHistory.paymentDate = paymentDate;
        return savingHistory;
    }
}
