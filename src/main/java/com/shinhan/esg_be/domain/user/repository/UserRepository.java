package com.shinhan.esg_be.domain.user.repository;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    interface GradeDistributionProjection {
        Grade getCurrentGrade();
        long getCount();
    }

    Optional<User> findFirstByOrderByUserIdAsc();

    Optional<User> findByLoginId(String loginId);

    long countByIsActiveTrue();

    boolean existsByLoginId(String loginId);

    boolean existsByCiDi(String ciDi);

    // 현재 사용자를 제외하고 같은 ciDi가 있는지 확인
    boolean existsByCiDiAndUserIdNot(String ciDi, Long userId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    // 현재 사용자를 제외하고 같은 전화번호가 있는지 확인
    boolean existsByPhoneNumberAndUserIdNot(String phoneNumber, Long userId);

    Optional<User> findByCiDi(String ciDi);

    boolean existsByLoginIdAndUserIdNot(String loginId, Long userId);

    boolean existsByEmailAndUserIdNot(String email, Long userId);

    @Query("""
            SELECT COUNT(u)
            FROM User u
            WHERE u.createdAt >= :startDateTime
              AND u.createdAt < :endDateTime
            """)
    long countCreatedAtBetween(
            @Param("startDateTime") java.time.LocalDateTime startDateTime,
            @Param("endDateTime") java.time.LocalDateTime endDateTime
    );

    @Query("""
            SELECT u.currentGrade AS currentGrade, COUNT(u) AS count
            FROM User u
            GROUP BY u.currentGrade
            """)
    List<GradeDistributionProjection> countGroupByCurrentGrade();

    @Query("""
            SELECT u
            FROM User u
            WHERE :keyword = ''
               OR LOWER(u.loginId) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<User> searchAdminUsers(@Param("keyword") String keyword, Pageable pageable);
}
