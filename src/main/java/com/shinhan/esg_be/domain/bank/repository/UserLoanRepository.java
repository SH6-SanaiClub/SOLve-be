package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLoanRepository extends JpaRepository<UserLoan, Long> {

    Optional<UserLoan> findByUser_UserIdAndStatus(Long userId, LoanStatus status);
}
