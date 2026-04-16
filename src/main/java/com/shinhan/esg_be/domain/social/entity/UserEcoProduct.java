package com.shinhan.esg_be.domain.social.entity;

import com.shinhan.esg_be.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_eco_product")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEcoProduct {

    @Id
    @Column(name = "purchase_id")
    private Long purchaseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "purchase_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct ecoProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static UserEcoProduct create(Payment payment, EcoProduct ecoProduct, User user, String deliveryAddress) {
        UserEcoProduct userEcoProduct = new UserEcoProduct();
        userEcoProduct.payment = payment;
        userEcoProduct.ecoProduct = ecoProduct;
        userEcoProduct.user = user;
        userEcoProduct.deliveryAddress = deliveryAddress;
        return userEcoProduct;
    }
}
