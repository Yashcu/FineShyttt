package com.yash.fineshyttt.service.product;

import com.yash.fineshyttt.domain.Inventory;
import com.yash.fineshyttt.domain.ProductVariant;
import com.yash.fineshyttt.exception.ResourceNotFoundException;
import com.yash.fineshyttt.repository.InventoryRepository;
import com.yash.fineshyttt.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository variantRepository;

    public Inventory setStock(Long variantId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Variant not found")
                );

        Inventory inventory = inventoryRepository
                .findByVariant_Id(variantId)
                .orElse(
                        Inventory.builder()
                                .variant(variant)
                                .quantity(0)
                                .build()
                );

        inventory.setQuantity(quantity);
        return inventoryRepository.save(inventory);
    }
}
