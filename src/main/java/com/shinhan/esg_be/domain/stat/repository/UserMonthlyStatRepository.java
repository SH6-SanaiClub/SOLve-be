package com.shinhan.esg_be.domain.stat.repository;

import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.shinhan.esg_be.domain.user.entity.User;

import java.util.Optional;
import java.util.List;

public interface UserMonthlyStatRepository extends JpaRepository<UserMonthlyStat, Long> {

    // 이번 달 통계 조회 (월 한도 필터용)
    @Query("""
            SELECT ums FROM UserMonthlyStat ums
            WHERE ums.user.userId = :userId
              AND ums.createdAt >= :monthStart
              AND ums.createdAt < :monthEnd
            """)
    Optional<UserMonthlyStat> findByUserAndMonth(
            @Param("userId") Long userId,
            @Param("monthStart") java.time.LocalDateTime monthStart,
            @Param("monthEnd") java.time.LocalDateTime monthEnd
    );
    
    Optional<UserMonthlyStat> findByUser(User user);

    List<UserMonthlyStat> findTop3ByUser_UserIdOrderByCreatedAtDesc(Long userId);

    long countByUser_UserId(Long userId);

    Optional<UserMonthlyStat> findTop1ByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
