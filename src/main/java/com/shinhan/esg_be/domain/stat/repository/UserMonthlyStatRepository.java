package com.shinhan.esg_be.domain.stat.repository;

import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserMonthlyStatRepository extends JpaRepository<UserMonthlyStat, Long> {

    Optional<UserMonthlyStat> findByUser(User user);
}
