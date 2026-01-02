package com.yash.fineshyttt.controller.admin;

import com.yash.fineshyttt.domain.Order;
import com.yash.fineshyttt.domain.OrderStatus;
import com.yash.fineshyttt.dto.order.OrderResponse;
import com.yash.fineshyttt.security.UserPrincipal;
import com.yash.fineshyttt.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OrderAdminController {

    private final OrderService orderService;

    /**
     * Update order status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) String notes
    ) {
        Order order = orderService.updateOrderStatus(
                orderId,
                status,
                principal.getUser(),
                notes
        );

        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
