package com.yash.fineshyttt.domain;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    /**
     * Check if order can be cancelled in this status
     */
    public boolean isCancellable() {
        return this == CREATED ||
                this == PAYMENT_PENDING ||
                this == PAID;
    }
}
