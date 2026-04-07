package com.shinhan.esg_be.domain.social.repository;

import com.shinhan.esg_be.domain.social.dto.response.DonationDetailResponse;
import com.shinhan.esg_be.domain.social.dto.response.DonationItemResponse;
import com.shinhan.esg_be.domain.social.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    @Query("""
            SELECT d
            FROM Donation d
            WHERE d.isActive = true
              AND d.endDate >= :baseDateTime
            ORDER BY d.endDate ASC
            """)
    List<Donation> findActiveDonations(@Param("baseDateTime") LocalDateTime baseDateTime);

    @Query("""
            select new com.shinhan.esg_be.domain.social.dto.response.DonationItemResponse(
                d.donationId,
                d.name,
                d.summary,
                d.targetAmount,
                d.currentAmount,
                d.imageUrl,
                count(ud),
                d.startDate,
                d.endDate
            )
            from Donation d
            left join UserDonation ud on ud.donation = d
            where d.isActive = true
              and d.endDate >= :baseDateTime
            group by d.donationId, d.name, d.summary, d.targetAmount, d.currentAmount,
                     d.imageUrl, d.startDate, d.endDate, d.isActive
            order by d.endDate asc
            """)
    List<DonationItemResponse> findActiveDonationList(@Param("baseDateTime") LocalDateTime baseDateTime);

    @Query("""
            select new com.shinhan.esg_be.domain.social.dto.response.DonationDetailResponse(
                d.donationId,
                d.name,
                d.summary,
                d.description,
                d.targetAmount,
                d.currentAmount,
                d.imageUrl,
                count(ud),
                d.startDate,
                d.endDate
            )
            from Donation d
            left join UserDonation ud on ud.donation = d
            where d.donationId = :donationId
              and d.isActive = true
              and d.endDate >= :baseDateTime
            group by d.donationId, d.name, d.summary, d.description, d.targetAmount,
                     d.currentAmount, d.imageUrl, d.startDate, d.endDate, d.isActive
            """)
    Optional<DonationDetailResponse> findActiveDonationDetail(
            @Param("donationId") Long donationId,
            @Param("baseDateTime") LocalDateTime baseDateTime
    );
}
