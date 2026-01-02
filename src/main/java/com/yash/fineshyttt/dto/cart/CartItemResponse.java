package com.yash.fineshyttt.dto.cart;

import com.yash.fineshyttt.domain.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long variantId,
        String productName,
        String variantSku,
        String size,
        String color,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal
) {
    public static CartItemResponse from(CartItem item) {
        var variant = item.getVariant();
        var product = variant.getProduct();

        BigDecimal price = variant.getPrice();
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                product.getName(),
                variant.getSku(),
                variant.getSize(),
                variant.getColor(),
                price,
                item.getQuantity(),
                subtotal
        );
    }
}
