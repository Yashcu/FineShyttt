package com.yash.fineshyttt.repository;

import com.yash.fineshyttt.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCart_IdAndVariant_Id(Long cartId, Long variantId);

    void deleteAllByCart_Id(Long cartId);
}
