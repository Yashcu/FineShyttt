package com.yash.fineshyttt.config;

/**
 * Centralized API endpoint constants.
 * Enforces consistent URL structure across the application.
 *
 * Pattern: {BASE}/{FEATURE}[/{OPERATION}]
 *
 * Benefits:
 * - Single source of truth for API versioning
 * - Compile-time checking (typos caught early)
 * - Easy to update all endpoints at once
 * - Supports future API versioning (v2, v3, etc.)
 */
public final class ApiConstants {

    // =========================
    // BASE PATHS
    // =========================
    public static final String API_V1 = "/api/v1";

    // =========================
    // AUTH ENDPOINTS
    // =========================
    public static final String AUTH_BASE = API_V1 + "/auth";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_REGISTER = "/register";
    public static final String AUTH_REFRESH = "/refresh";
    public static final String AUTH_LOGOUT = "/logout";
    public static final String AUTH_VERIFY = "/verify";

    // =========================
    // PRODUCT ENDPOINTS (Public)
    // =========================
    public static final String PRODUCTS_BASE = API_V1 + "/products";
    public static final String PRODUCTS_BY_SLUG = "/{slug}";
    public static final String PRODUCTS_FILTERS = "/filters";

    // =========================
    // CATEGORY ENDPOINTS (Public)
    // =========================
    public static final String CATEGORIES_BASE = API_V1 + "/categories";

    // =========================
    // CART ENDPOINTS
    // =========================
    public static final String CART_BASE = API_V1 + "/cart";
    public static final String CART_ITEMS = "/items";
    public static final String CART_ITEM_BY_VARIANT = "/items/{variantId}";

    // =========================
    // ORDER ENDPOINTS
    // =========================
    public static final String ORDERS_BASE = API_V1 + "/orders";
    public static final String ORDERS_BY_ID = "/{orderId}";
    public static final String ORDERS_CANCEL = "/{orderId}/cancel";

    // =========================
    // ADMIN ENDPOINTS
    // =========================
    public static final String ADMIN_BASE = API_V1 + "/admin";

    // Admin - Products
    public static final String ADMIN_PRODUCTS = ADMIN_BASE + "/products";
    public static final String ADMIN_PRODUCTS_BY_ID = "/{productId}";
    public static final String ADMIN_PRODUCTS_IMAGES = "/{productId}/images";
    public static final String ADMIN_PRODUCTS_STOCK = "/variants/{variantId}/stock";

    // Admin - Orders
    public static final String ADMIN_ORDERS = ADMIN_BASE + "/orders";
    public static final String ADMIN_ORDERS_BY_ID = "/{orderId}";
    public static final String ADMIN_ORDERS_STATUS = "/{orderId}/status";

    // =========================
    // HEALTH & MONITORING
    // =========================
    public static final String HEALTH = "/api/health";
    public static final String ACTUATOR = "/actuator/**";

    // =========================
    // PUBLIC ENDPOINTS (for SecurityConfig)
    // =========================
    public static final String[] PUBLIC_ENDPOINTS = {
            AUTH_BASE + "/**",
            PRODUCTS_BASE + "/**",
            CATEGORIES_BASE + "/**",
            HEALTH,
            "/actuator/health"
    };

    private ApiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
