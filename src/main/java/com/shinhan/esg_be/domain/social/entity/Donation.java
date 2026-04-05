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
}
