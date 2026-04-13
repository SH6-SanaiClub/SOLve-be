package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserLoanRepository extends JpaRepository<UserLoan, Long> {

    List<UserLoan> findByUserAndStatus(User user, LoanStatus status);

    @EntityGraph(attributePaths = {"user", "financialProduct"})
    List<UserLoan> findByStatusAndNextRepaymentDateLessThanEqual(LoanStatus status, LocalDate repaymentDate);

    @EntityGraph(attributePaths = "financialProduct")
    Optional<UserLoan> findByUser_UserIdAndStatus(Long userId, LoanStatus status);

    @EntityGraph(attributePaths = "financialProduct")
    List<UserLoan> findAllByUser_UserIdAndStatus(Long userId, LoanStatus status);

    @EntityGraph(attributePaths = "financialProduct")
    List<UserLoan> findAllByUser_UserId(Long userId);

    boolean existsByUserAndFinancialProductAndStatus(
            User user,
            FinancialProduct financialProduct,
            LoanStatus status
    );
}
