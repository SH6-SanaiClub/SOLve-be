package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.UserSaving;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSavingRepository extends JpaRepository<UserSaving, Long> {

    Optional<UserSaving> findByUser_UserIdAndStatus(Long userId, SavingStatus status);
}
