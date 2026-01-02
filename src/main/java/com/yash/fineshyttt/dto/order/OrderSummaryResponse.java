package com.yash.fineshyttt.dto.order;

import com.yash.fineshyttt.domain.Order;
import com.yash.fineshyttt.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(
        Long id,
        OrderStatus status,
        int itemCount,
        BigDecimal totalAmount,
        Instant createdAt
) {
    public static OrderSummaryResponse from(Order order) {
        int itemCount = order.getItems()
                .stream()
                .mapToInt(item -> item.getQuantity())
                .sum();

        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                itemCount,
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
