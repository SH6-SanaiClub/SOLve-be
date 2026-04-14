package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.SavingHistory;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingHistoryType;
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

    @Query("""
            select savingHistory
            from SavingHistory savingHistory
            join fetch savingHistory.userSaving userSaving
            join fetch userSaving.financialProduct
            where userSaving.user.userId = :userId
              and userSaving.savingId = :savingId
            order by savingHistory.paymentDate desc
            """)
    List<SavingHistory> findAllByUserIdAndSavingId(Long userId, Long savingId);

    @Query("""
            select count(savingHistory)
            from SavingHistory savingHistory
            where savingHistory.userSaving.savingId = :savingId
              and savingHistory.type = :type
            """)
    long countBySavingIdAndType(Long savingId, SavingHistoryType type);

    @Query("""
            select coalesce(sum(savingHistory.amount), 0)
            from SavingHistory savingHistory
            where savingHistory.userSaving.savingId = :savingId
              and savingHistory.type = :type
            """)
    long sumAmountBySavingIdAndType(Long savingId, SavingHistoryType type);
}
