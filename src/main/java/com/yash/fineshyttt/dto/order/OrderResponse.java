package com.yash.fineshyttt.dto.order;

import com.yash.fineshyttt.domain.Order;
import com.yash.fineshyttt.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        Long shippingAddressId,
        Long billingAddressId,
        String couponCode,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        BigDecimal finalAmount = order.getTotalAmount()
                .subtract(order.getDiscountAmount());

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                items,
                order.getTotalAmount(),
                order.getDiscountAmount(),
                finalAmount,
                order.getShippingAddress().getId(),
                order.getBillingAddress().getId(),
                order.getCoupon() != null ? order.getCoupon().getCode() : null,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
