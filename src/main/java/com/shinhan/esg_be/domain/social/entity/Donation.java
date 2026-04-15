package com.shinhan.esg_be.domain.social.entity;

import com.shinhan.esg_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "donation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Donation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donation_id")
    private Long donationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_amount", nullable = false)
    private Long targetAmount;

    @Column(name = "current_amount", nullable = false)
    private Long currentAmount;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    public static Donation create(
            String name,
            String summary,
            String description,
            Long targetAmount,
            String imageUrl,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Boolean isActive
    ) {
        Donation donation = new Donation();
        donation.name = name;
        donation.summary = summary;
        donation.description = description;
        donation.targetAmount = targetAmount;
        donation.currentAmount = 0L;
        donation.imageUrl = imageUrl;
        donation.startDate = startDate;
        donation.endDate = endDate;
        donation.isActive = isActive;
        return donation;
    }

    public void update(
            String name,
            String summary,
            String description,
            Long targetAmount,
            String imageUrl,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.targetAmount = targetAmount;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateStatus(boolean isActive) {
        this.isActive = isActive;
    }
}
