package com.shinhan.esg_be.domain.stat.repository;

import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserMonthlyStatRepository extends JpaRepository<UserMonthlyStat, Long> {

    // 이번 달 통계 조회 (월 한도 필터용)
    @Query("""
            SELECT ums FROM UserMonthlyStat ums
            WHERE ums.user.userId = :userId
              AND YEAR(ums.createdAt) = :year
              AND MONTH(ums.createdAt) = :month
            """)
    Optional<UserMonthlyStat> findByUserAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );
}
