package com.yash.fineshyttt.controller.order;

import com.yash.fineshyttt.config.ApiConstants;
import com.yash.fineshyttt.domain.Order;
import com.yash.fineshyttt.dto.order.CheckoutRequest;
import com.yash.fineshyttt.dto.order.OrderResponse;
import com.yash.fineshyttt.dto.order.OrderSummaryResponse;
import com.yash.fineshyttt.security.UserPrincipal;
import com.yash.fineshyttt.service.order.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.ORDERS_BASE)
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Checkout - Create order from cart
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutRequest request
    ) {
        Order order = orderService.checkout(
                principal.getUser(),
                request.shippingAddressId(),
                request.billingAddressId(),
                request.couponCode()
        );

        return ResponseEntity.status(201)
                .body(OrderResponse.from(order));
    }

    /**
     * Get user's orders (with pagination)
     */
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<Order> orders = orderService.getUserOrders(
                principal.getUser(),
                pageable
        );

        return ResponseEntity.ok(
                orders.map(OrderSummaryResponse::from)
        );
    }

    /**
     * Get single order details
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId
    ) {
        Order order = orderService.getOrder(
                orderId,
                principal.getUser()
        );

        return ResponseEntity.ok(OrderResponse.from(order));
    }

    /**
     * Cancel order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId
    ) {
        Order order = orderService.cancelOrder(
                orderId,
                principal.getUser()
        );

        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
