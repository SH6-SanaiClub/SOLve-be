package com.shinhan.esg_be.domain.social.repository;

import com.shinhan.esg_be.domain.recommendation.dto.DonationCountProjection;
import com.shinhan.esg_be.domain.social.dto.response.DonationHistoryItemResponse;
import com.shinhan.esg_be.domain.social.entity.UserDonation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserDonationRepository extends JpaRepository<UserDonation, Long> {

    @Query("""
            SELECT COUNT(ud)
            FROM UserDonation ud
            WHERE ud.createdAt >= :startDateTime
              AND ud.createdAt < :endDateTime
            """)
    long countCreatedAtBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
            SELECT COALESCE(SUM(ud.payment.amount), 0)
            FROM UserDonation ud
            WHERE ud.createdAt >= :startDateTime
              AND ud.createdAt < :endDateTime
            """)
    long sumPaymentAmountCreatedAtBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    long countByDonation_DonationId(Long donationId);

    @Query("""
            SELECT ud
            FROM UserDonation ud
            JOIN FETCH ud.user u
            JOIN FETCH ud.payment p
            WHERE ud.donation.donationId = :donationId
            ORDER BY ud.createdAt DESC
            """)
    List<UserDonation> findAllByDonationIdWithUserAndPayment(@Param("donationId") Long donationId);

    // 오늘 특정 캠페인 기부 여부 (하드 필터)
    @Query("""
            SELECT COUNT(ud) FROM UserDonation ud
            WHERE ud.user.userId = :userId
              AND ud.donation.donationId = :donationId
              AND ud.createdAt >= :startOfDay
            """)
    long countTodayByDonation(
            @Param("userId") Long userId,
            @Param("donationId") Long donationId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    // 최근 N일 기부 횟수 (미활동 위험, 90일 횟수)
    @Query("""
            SELECT COUNT(ud) FROM UserDonation ud
            WHERE ud.user.userId = :userId
              AND ud.createdAt >= :since
            """)
    long countSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    long countByUser_UserIdAndDonation_DonationIdAndCreatedAtAfter(
            Long userId,
            Long donationId,
            LocalDateTime since
    );

    @Query("""
            SELECT ud.donation.donationId AS donationId, COUNT(ud) AS count
            FROM UserDonation ud
            WHERE ud.createdAt >= :since
            GROUP BY ud.donation.donationId
            """)
    List<DonationCountProjection> countGroupByDonationSince(
            @Param("since") LocalDateTime since
    );

    @Query("""
            select new com.shinhan.esg_be.domain.social.dto.response.DonationHistoryItemResponse(
                ud.donationLogId,
                d.donationId,
                d.organization,
                d.name,
                d.imageUrl,
                p.paidAt,
                p.amount
            )
            from UserDonation ud
            join ud.donation d
            join ud.payment p
            where ud.user.userId = :userId
            order by p.paidAt desc
            """)
    List<DonationHistoryItemResponse> findDonationHistoryByUserId(@Param("userId") Long userId);
}
