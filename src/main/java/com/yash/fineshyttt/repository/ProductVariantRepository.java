package com.yash.fineshyttt.repository;

import com.yash.fineshyttt.domain.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);

    @EntityGraph(attributePaths = {"category"})
    List<ProductVariant> findAllByProduct_IdAndIsActiveTrue(Long productId);
}
