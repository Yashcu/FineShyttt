package com.yash.fineshyttt.repository;

import com.yash.fineshyttt.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByVariant_Id(Long variantId);
}
