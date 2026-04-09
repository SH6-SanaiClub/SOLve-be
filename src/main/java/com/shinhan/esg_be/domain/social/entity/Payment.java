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
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "imp_uid", nullable = false)
    private String impUid;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "failed_reason")
    private String failedReason;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    @Column(name = "buyer_email", nullable = false)
    private String buyerEmail;

    @Column(name = "buyer_tel", nullable = false)
    private String buyerTel;

    @Column(name = "pg_provider", nullable = false)
    private String pgProvider;

    @Column(name = "pg_tid", nullable = false)
    private String pgTid;

    @Column(name = "card_name", nullable = false)
    private String cardName;

    @Column(name = "card_number", nullable = false)
    private String cardNumber;

    @Column(name = "receipt_url", nullable = false)
    private String receiptUrl;

    public static Payment create(
            String impUid,
            String merchantId,
            Long amount,
            String paymentMethod,
            String paymentStatus,
            LocalDateTime paidAt,
            String failedReason,
            String buyerName,
            String buyerEmail,
            String buyerTel,
            String pgProvider,
            String pgTid,
            String cardName,
            String cardNumber,
            String receiptUrl
    ) {
        Payment payment = new Payment();
        payment.impUid = impUid;
        payment.merchantId = merchantId;
        payment.amount = amount;
        payment.paymentMethod = paymentMethod;
        payment.paymentStatus = paymentStatus;
        payment.paidAt = paidAt;
        payment.failedReason = failedReason;
        payment.buyerName = buyerName;
        payment.buyerEmail = buyerEmail;
        payment.buyerTel = buyerTel;
        payment.pgProvider = pgProvider;
        payment.pgTid = pgTid;
        payment.cardName = cardName;
        payment.cardNumber = cardNumber;
        payment.receiptUrl = receiptUrl;
        return payment;
    }
}
