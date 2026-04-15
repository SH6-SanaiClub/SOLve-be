package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.UserLoan;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserLoanRepository extends JpaRepository<UserLoan, Long> {

    long countByStatus(LoanStatus status);

    long countByFinancialProduct_FinProductIdAndStatus(Long finProductId, LoanStatus status);

    List<UserLoan> findByUserAndStatus(User user, LoanStatus status);

    @EntityGraph(attributePaths = {"user", "financialProduct"})
    List<UserLoan> findByStatusAndNextRepaymentDateLessThanEqual(LoanStatus status, LocalDate repaymentDate);

    @EntityGraph(attributePaths = "financialProduct")
    Optional<UserLoan> findByUser_UserIdAndStatus(Long userId, LoanStatus status);

    boolean existsByUser_UserIdAndStatus(Long userId, LoanStatus status);
  
    @EntityGraph(attributePaths = "financialProduct")
    List<UserLoan> findAllByUser_UserIdAndStatus(Long userId, LoanStatus status);

    @EntityGraph(attributePaths = "financialProduct")
    List<UserLoan> findAllByUser_UserId(Long userId);

    @EntityGraph(attributePaths = {"user", "financialProduct"})
    List<UserLoan> findAllByFinancialProduct_FinProductId(Long finProductId);

    boolean existsByUserAndFinancialProductAndStatus(
            User user,
            FinancialProduct financialProduct,
            LoanStatus status
    );

    @Query("""
            SELECT COALESCE(SUM(ul.principalAmount), 0)
            FROM UserLoan ul
            WHERE ul.status = :status
            """)
    long sumPrincipalAmountByStatus(@Param("status") LoanStatus status);
}
