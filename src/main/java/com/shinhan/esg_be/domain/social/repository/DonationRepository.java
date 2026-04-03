package com.shinhan.esg_be.domain.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.shinhan.esg_be.domain.social.entity.Donation;

import java.time.LocalDateTime;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    @Query("""
            select
                d.donationId as donationId,
                d.name as name,
                d.description as description,
                d.targetAmount as targetAmount,
                d.currentAmount as currentAmount,
                d.imageUrl as imageUrl,
                d.startDate as startDate,
                d.endDate as endDate,
                d.isActive as active,
                count(ud) as participantCount
            from Donation d
            left join UserDonation ud on ud.donation = d
            where d.isActive = true
              and d.endDate >= :baseDateTime
            group by d.donationId, d.name, d.description, d.targetAmount, d.currentAmount,
                     d.imageUrl, d.startDate, d.endDate, d.isActive
            order by d.endDate asc
            """)
    List<DonationListProjection> findActiveDonationList(@Param("baseDateTime") LocalDateTime baseDateTime);
}
