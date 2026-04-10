package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.SavingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SavingHistoryRepository extends JpaRepository<SavingHistory, Long> {

    @Query("""
            select savingHistory
            from SavingHistory savingHistory
            join fetch savingHistory.userSaving userSaving
            join fetch userSaving.financialProduct
            where userSaving.user.userId = :userId
            order by savingHistory.paymentDate desc
            """)
    List<SavingHistory> findAllByUserId(Long userId);

    long countByUserSaving_SavingId(Long savingId);

    @Query("""
            select coalesce(sum(savingHistory.amount), 0)
            from SavingHistory savingHistory
            where savingHistory.userSaving.savingId = :savingId
            """)
    long sumAmountBySavingId(Long savingId);
}
