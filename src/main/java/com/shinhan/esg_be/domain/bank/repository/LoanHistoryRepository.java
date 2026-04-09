package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.LoanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanHistoryRepository extends JpaRepository<LoanHistory, Long> {

    @Query("""
            select loanHistory
            from LoanHistory loanHistory
            join fetch loanHistory.userLoan userLoan
            join fetch userLoan.financialProduct
            where userLoan.user.userId = :userId
            order by loanHistory.paymentDate desc
            """)
    List<LoanHistory> findAllByUserId(Long userId);
}
