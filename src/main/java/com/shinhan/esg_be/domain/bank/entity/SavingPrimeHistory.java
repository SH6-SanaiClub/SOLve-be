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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "saving_prime_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavingPrimeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prime_id")
    private Long primeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saving_id", nullable = false)
    private UserSaving userSaving;

    @Column(name = "added_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal addedRate;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;
}
