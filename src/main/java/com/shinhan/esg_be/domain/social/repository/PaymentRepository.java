package com.shinhan.esg_be.domain.social.repository;

import com.shinhan.esg_be.domain.social.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByImpUid(String impUid);

    boolean existsByMerchantId(String merchantId);
}
