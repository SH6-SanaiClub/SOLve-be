package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSavingRepository extends JpaRepository<UserSaving, Long> {

    @EntityGraph(attributePaths = "financialProduct")
    Optional<UserSaving> findByUser_UserIdAndStatus(Long userId, SavingStatus status);

    @Query("SELECT us.financialProduct.name FROM UserSaving us " +
            "WHERE us.user.userId = :userId AND us.status = 'ACTIVE'")
    List<String> findActiveProductNamesByUserId(@Param("userId") Long userId);
  
    @EntityGraph(attributePaths = "financialProduct")
    List<UserSaving> findAllByUser_UserIdAndStatus(Long userId, SavingStatus status);

    @EntityGraph(attributePaths = "financialProduct")
    List<UserSaving> findAllByUser_UserId(Long userId);

    List<UserSaving> findByStatus(SavingStatus status);

    boolean existsByUser_UserIdAndStatus(Long userId, SavingStatus status);

    boolean existsByUserAndFinancialProductAndStatus(
            User user,
            FinancialProduct financialProduct,
            SavingStatus status
    );
}
