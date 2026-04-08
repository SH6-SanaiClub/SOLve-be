package com.shinhan.esg_be.domain.bank.repository;

import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialProductRepository extends JpaRepository<FinancialProduct, Long> {

    List<FinancialProduct> findByTypeAndIsActiveTrue(ProductType type);

    Optional<FinancialProduct> findByFinProductIdAndTypeAndIsActiveTrue(Long finProductId, ProductType type);
}
