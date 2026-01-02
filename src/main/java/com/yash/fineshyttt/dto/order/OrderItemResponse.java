package com.yash.fineshyttt.dto.order;

import com.yash.fineshyttt.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long variantId,
        String productName,
        String variantSku,
        String size,
        String color,
        int quantity,
        BigDecimal priceAtPurchase,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        var variant = item.getVariant();
        var product = variant.getProduct();

        BigDecimal subtotal = item.getPriceAtPurchase()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new OrderItemResponse(
                item.getId(),
                variant.getId(),
                product.getName(),
                variant.getSku(),
                variant.getSize(),
                variant.getColor(),
                item.getQuantity(),
                item.getPriceAtPurchase(),
                subtotal
        );
    }
}
