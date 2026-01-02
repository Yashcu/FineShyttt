package com.yash.fineshyttt.dto.order;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "Shipping address is required")
        Long shippingAddressId,

        @NotNull(message = "Billing address is required")
        Long billingAddressId,

        String couponCode
) {}
