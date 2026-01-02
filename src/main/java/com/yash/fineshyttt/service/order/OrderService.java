package com.yash.fineshyttt.service.order;

import com.yash.fineshyttt.domain.*;
import com.yash.fineshyttt.exception.ResourceNotFoundException;
import com.yash.fineshyttt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    /**
     * Create order from user's cart
     */
    public Order checkout(
            User user,
            Long shippingAddressId,
            Long billingAddressId,
            String couponCode
    ) {
        // 1. Get user's cart
        Cart cart = cartRepository.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart not found")
                );

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // 2. Validate addresses
        Address shippingAddress = addressRepository
                .findByIdAndUser_Id(shippingAddressId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Shipping address not found")
                );

        Address billingAddress = addressRepository
                .findByIdAndUser_Id(billingAddressId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Billing address not found")
                );

        // 3. Validate stock and reserve inventory
        for (var cartItem : cart.getItems()) {
            Inventory inventory = inventoryRepository
                    .findByVariant_Id(cartItem.getVariant().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Inventory not found")
                    );

            int availableStock = inventory.getQuantity() - inventory.getReservedQuantity();

            if (availableStock < cartItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " +
                                cartItem.getVariant().getProduct().getName()
                );
            }

            // Reserve inventory
            inventory.setReservedQuantity(
                    inventory.getReservedQuantity() + cartItem.getQuantity()
            );
            inventoryRepository.save(inventory);
        }

        // 4. Calculate total
        BigDecimal totalAmount = cart.getItems()
                .stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Apply coupon if provided
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (couponCode != null && !couponCode.isBlank()) {
            coupon = validateAndApplyCoupon(couponCode, totalAmount);
            discountAmount = calculateDiscount(coupon, totalAmount);
        }

        // 6. Create order
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.CREATED)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .coupon(coupon)
                .build();

        // 7. Create order items
        for (var cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(cartItem.getVariant())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(cartItem.getPrice())
                    .build();

            order.getItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // 8. Record status history
        recordStatusChange(savedOrder, null, OrderStatus.CREATED, user);

        // 9. Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }

    /**
     * Get order by ID (user must own the order)
     */
    @Transactional(readOnly = true)
    public Order getOrder(Long orderId, User user) {
        return orderRepository.findByIdAndUser_Id(orderId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found")
                );
    }

    /**
     * Get all orders for user
     */
    @Transactional(readOnly = true)
    public Page<Order> getUserOrders(User user, Pageable pageable) {
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(
                user.getId(),
                pageable
        );
    }

    /**
     * Update order status (admin only)
     */
    public Order updateOrderStatus(
            Long orderId,
            OrderStatus newStatus,
            User changedBy,
            String notes
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found")
                );

        OrderStatus oldStatus = order.getStatus();

        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + oldStatus + " -> " + newStatus
            );
        }

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // Record status change
        recordStatusChange(savedOrder, oldStatus, newStatus, changedBy, notes);

        // Handle inventory based on status
        handleInventoryOnStatusChange(order, oldStatus, newStatus);

        return savedOrder;
    }

    /**
     * Cancel order
     */
    public Order cancelOrder(Long orderId, User user) {
        Order order = getOrder(orderId, user);

        if (!order.getStatus().isCancellable()) {
            throw new IllegalArgumentException(
                    "Order cannot be cancelled in current status: " + order.getStatus()
            );
        }

        return updateOrderStatus(
                orderId,
                OrderStatus.CANCELLED,
                user,
                "Cancelled by user"
        );
    }

    // ==============================
    // PRIVATE HELPERS
    // ==============================

    private Coupon validateAndApplyCoupon(String code, BigDecimal totalAmount) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Coupon not found")
                );

        if (!coupon.getIsActive()) {
            throw new IllegalArgumentException("Coupon is not active");
        }

        if (coupon.getValidUntil().isBefore(java.time.Instant.now())) {
            throw new IllegalArgumentException("Coupon has expired");
        }

        if (coupon.getMinOrderAmount() != null &&
                totalAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Minimum order amount not met: " + coupon.getMinOrderAmount()
            );
        }

        if (coupon.getUsageLimit() != null &&
                coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            throw new IllegalArgumentException("Coupon usage limit reached");
        }

        // Increment usage
        coupon.setTimesUsed(coupon.getTimesUsed() + 1);
        couponRepository.save(coupon);

        return coupon;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal totalAmount) {
        BigDecimal discount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = totalAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));
        } else {
            discount = coupon.getDiscountValue();
        }

        // Apply max discount limit
        if (coupon.getMaxDiscount() != null &&
                discount.compareTo(coupon.getMaxDiscount()) > 0) {
            discount = coupon.getMaxDiscount();
        }

        return discount;
    }

    private void recordStatusChange(
            Order order,
            OrderStatus oldStatus,
            OrderStatus newStatus,
            User changedBy
    ) {
        recordStatusChange(order, oldStatus, newStatus, changedBy, null);
    }

    private void recordStatusChange(
            Order order,
            OrderStatus oldStatus,
            OrderStatus newStatus,
            User changedBy,
            String notes
    ) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .notes(notes)
                .build();

        orderStatusHistoryRepository.save(history);
    }

    private boolean isValidStatusTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case CREATED -> to == OrderStatus.PAYMENT_PENDING ||
                    to == OrderStatus.CANCELLED;
            case PAYMENT_PENDING -> to == OrderStatus.PAID ||
                    to == OrderStatus.CANCELLED;
            case PAID -> to == OrderStatus.SHIPPED ||
                    to == OrderStatus.REFUNDED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED -> to == OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };
    }

    private void handleInventoryOnStatusChange(
            Order order,
            OrderStatus oldStatus,
            OrderStatus newStatus
    ) {
        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REFUNDED) {
            // Release reserved inventory
            for (var item : order.getItems()) {
                Inventory inventory = inventoryRepository
                        .findByVariant_Id(item.getVariant().getId())
                        .orElseThrow();

                inventory.setReservedQuantity(
                        Math.max(0, inventory.getReservedQuantity() - item.getQuantity())
                );
                inventoryRepository.save(inventory);
            }
        } else if (newStatus == OrderStatus.PAID) {
            // Deduct from actual inventory
            for (var item : order.getItems()) {
                Inventory inventory = inventoryRepository
                        .findByVariant_Id(item.getVariant().getId())
                        .orElseThrow();

                inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
                inventory.setReservedQuantity(
                        Math.max(0, inventory.getReservedQuantity() - item.getQuantity())
                );
                inventoryRepository.save(inventory);
            }
        }
    }
}
