package com.shinhan.esg_be.domain.social.repository;

import com.shinhan.esg_be.domain.social.dto.response.DonationItemResponse;
import com.shinhan.esg_be.domain.social.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    @Query("""
            select new com.shinhan.esg_be.domain.social.dto.response.DonationItemResponse(
                d.donationId,
                d.name,
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
            where d.isActive = true
              and d.endDate >= :baseDateTime
            group by d.donationId, d.name, d.description, d.targetAmount, d.currentAmount,
                     d.imageUrl, d.startDate, d.endDate, d.isActive
            order by d.endDate asc
            """)
    List<DonationItemResponse> findActiveDonationList(@Param("baseDateTime") LocalDateTime baseDateTime);
}
