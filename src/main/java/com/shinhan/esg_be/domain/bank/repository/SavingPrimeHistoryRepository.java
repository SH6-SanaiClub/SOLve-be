package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.SavingPrimeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavingPrimeHistoryRepository extends JpaRepository<SavingPrimeHistory, Long> {

    Optional<SavingPrimeHistory> findTopByUserSaving_SavingIdOrderByAppliedAtDesc(Long savingId);
}
