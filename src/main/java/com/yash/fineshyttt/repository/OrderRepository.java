package com.yash.fineshyttt.repository;

import com.yash.fineshyttt.domain.Order;
import com.yash.fineshyttt.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {
            "items",
            "items.variant",
            "items.variant.product",
            "shippingAddress",
            "billingAddress",
            "coupon"
    })
    Optional<Order> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = {"items"})
    Page<Order> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByUser_Id(Long userId);
}
