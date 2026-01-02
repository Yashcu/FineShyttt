package com.yash.fineshyttt.repository;

import com.yash.fineshyttt.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category", "images"})
    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAllByIsActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAllByCategory_IdAndIsActiveTrue(
            Long categoryId,
            Pageable pageable
    );

    boolean existsBySlug(String candidate);
}
