package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.LoanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

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

    @Query("""
            select count(loanHistory)
            from LoanHistory loanHistory
            where loanHistory.userLoan.loanId = :loanId
              and loanHistory.amount < 0
            """)
    long countRepaymentsByLoanId(Long loanId);

    @Query("""
            select coalesce(sum(abs(loanHistory.amount)), 0)
            from LoanHistory loanHistory
            where loanHistory.userLoan.loanId = :loanId
              and loanHistory.amount < 0
            """)
    Long sumRepaymentAmountByLoanId(Long loanId);
}
