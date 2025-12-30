-- Flyway Migration: V1__init.sql
-- Purpose: Initial schema for Fineshyttt E-Commerce Backend
-- This file is GENERATED from DATABASE_SCHEMA.md (LOCKED)
-- Any modification requires a new migration

-- =========================
-- ENUM TYPES
-- =========================

CREATE TYPE order_status AS ENUM (
  'CREATED',
  'PAYMENT_PENDING',
  'PAID',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
  'REFUNDED'
);

CREATE TYPE payment_status AS ENUM (
  'PENDING',
  'SUCCESS',
  'FAILED'
);

CREATE TYPE discount_type AS ENUM (
  'PERCENTAGE',
  'FIXED'
);

CREATE TYPE address_type AS ENUM (
  'SHIPPING',
  'BILLING'
);

-- =========================
-- USERS & AUTH
-- =========================

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(100),
                       last_name VARCHAR(100),
                       phone_number VARCHAR(20),
                       email_verified BOOLEAN NOT NULL DEFAULT false,
                       is_enabled BOOLEAN NOT NULL DEFAULT true,
                       last_login_at TIMESTAMP,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL
);

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL REFERENCES users(id),
                            role_id BIGINT NOT NULL REFERENCES roles(id),
                            PRIMARY KEY (user_id, role_id)
);

CREATE TABLE addresses (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL REFERENCES users(id),
                           address_type address_type NOT NULL,
                           full_name VARCHAR(255) NOT NULL,
                           phone_number VARCHAR(20) NOT NULL,
                           address_line1 VARCHAR(255) NOT NULL,
                           address_line2 VARCHAR(255),
                           city VARCHAR(100) NOT NULL,
                           state VARCHAR(100) NOT NULL,
                           postal_code VARCHAR(20) NOT NULL,
                           country VARCHAR(100) NOT NULL,
                           is_default BOOLEAN NOT NULL DEFAULT false,
                           created_at TIMESTAMP NOT NULL,
                           updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);

-- =========================
-- PRODUCT CATALOG
-- =========================

CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            is_active BOOLEAN NOT NULL,
                            created_at TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP NOT NULL
);

CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          slug VARCHAR(255) NOT NULL UNIQUE,
                          description TEXT NOT NULL,
                          category_id BIGINT NOT NULL REFERENCES categories(id),
                          is_active BOOLEAN NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_active_category ON products(category_id) WHERE is_active = true;

CREATE TABLE product_images (
                                id BIGSERIAL PRIMARY KEY,
                                product_id BIGINT NOT NULL REFERENCES products(id),
                                image_url TEXT NOT NULL,
                                is_primary BOOLEAN NOT NULL DEFAULT false,
                                display_order INT NOT NULL,
                                created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);

CREATE TABLE product_variants (
                                  id BIGSERIAL PRIMARY KEY,
                                  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                  sku VARCHAR(100) NOT NULL UNIQUE,
                                  material VARCHAR(100) NOT NULL,
                                  color VARCHAR(50) NOT NULL,
                                  size VARCHAR(50),
                                  price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
                                  is_active BOOLEAN NOT NULL,
                                  created_at TIMESTAMP NOT NULL,
                                  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_product_variants_product_active ON product_variants(product_id, is_active);

CREATE TABLE inventory (
                           id BIGSERIAL PRIMARY KEY,
                           variant_id BIGINT NOT NULL UNIQUE REFERENCES product_variants(id),
                           quantity INT NOT NULL CHECK (quantity >= 0),
                           reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
                           updated_at TIMESTAMP NOT NULL
);

CREATE TABLE product_reviews (
                                 id BIGSERIAL PRIMARY KEY,
                                 product_id BIGINT NOT NULL REFERENCES products(id),
                                 user_id BIGINT NOT NULL REFERENCES users(id),
                                 rating SMALLINT CHECK (rating BETWEEN 1 AND 5),
                                 title VARCHAR(255),
                                 review_text TEXT,
                                 is_verified_purchase BOOLEAN NOT NULL DEFAULT false,
                                 is_approved BOOLEAN NOT NULL DEFAULT false,
                                 created_at TIMESTAMP NOT NULL,
                                 updated_at TIMESTAMP NOT NULL,
                                 UNIQUE (product_id, user_id)
);

CREATE INDEX idx_product_reviews_approved ON product_reviews(product_id) WHERE is_approved = true;

-- =========================
-- CART
-- =========================

CREATE TABLE carts (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT REFERENCES users(id),
                       session_id VARCHAR(255),
                       expires_at TIMESTAMP,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,
                       CHECK (
                           (user_id IS NOT NULL AND session_id IS NULL) OR
                           (user_id IS NULL AND session_id IS NOT NULL)
                           )
);

CREATE INDEX idx_carts_user_id ON carts(user_id);
CREATE INDEX idx_carts_session_id ON carts(session_id) WHERE session_id IS NOT NULL;

CREATE TABLE cart_items (
                            id BIGSERIAL PRIMARY KEY,
                            cart_id BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
                            variant_id BIGINT NOT NULL REFERENCES product_variants(id),
                            quantity INT NOT NULL CHECK (quantity > 0),
                            UNIQUE (cart_id, variant_id)
);

-- =========================
-- ORDERS
-- =========================

CREATE TABLE coupons (
                         id BIGSERIAL PRIMARY KEY,
                         code VARCHAR(50) NOT NULL UNIQUE,
                         discount_type discount_type NOT NULL,
                         discount_value NUMERIC(12,2) NOT NULL,
                         min_order_amount NUMERIC(12,2),
                         max_discount NUMERIC(12,2),
                         valid_from TIMESTAMP NOT NULL,
                         valid_until TIMESTAMP NOT NULL,
                         usage_limit INT,
                         times_used INT NOT NULL DEFAULT 0,
                         is_active BOOLEAN NOT NULL DEFAULT true,
                         created_at TIMESTAMP NOT NULL
);

CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL REFERENCES users(id),
                        status order_status NOT NULL,
                        total_amount NUMERIC(12,2) NOT NULL,
                        shipping_address_id BIGINT REFERENCES addresses(id),
                        billing_address_id BIGINT REFERENCES addresses(id),
                        coupon_id BIGINT REFERENCES coupons(id),
                        discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_orders_user_status ON orders(user_id, status);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL REFERENCES orders(id),
                             variant_id BIGINT NOT NULL REFERENCES product_variants(id),
                             quantity INT NOT NULL CHECK (quantity > 0),
                             price_at_purchase NUMERIC(12,2) NOT NULL CHECK (price_at_purchase >= 0)
);

CREATE TABLE order_status_history (
                                      id BIGSERIAL PRIMARY KEY,
                                      order_id BIGINT NOT NULL REFERENCES orders(id),
                                      old_status order_status,
                                      new_status order_status NOT NULL,
                                      changed_by BIGINT REFERENCES users(id),
                                      notes TEXT,
                                      created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history(order_id);

-- =========================
-- PAYMENTS
-- =========================

CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,
                          order_id BIGINT NOT NULL REFERENCES orders(id),
                          idempotency_key VARCHAR(100) NOT NULL UNIQUE,
                          amount NUMERIC(12,2) NOT NULL,
                          currency VARCHAR(3) NOT NULL DEFAULT 'INR',
                          payment_method VARCHAR(50) NOT NULL,
                          provider VARCHAR(50) NOT NULL,
                          provider_reference VARCHAR(255) NOT NULL,
                          status payment_status NOT NULL,
                          failure_reason TEXT,
                          expires_at TIMESTAMP,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,
                          UNIQUE (provider, provider_reference)
);

CREATE INDEX idx_payments_order_id ON payments(order_id);

-- =========================
-- WISHLIST
-- =========================

CREATE TABLE wishlists (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL REFERENCES users(id),
                           product_id BIGINT NOT NULL REFERENCES products(id),
                           created_at TIMESTAMP NOT NULL,
                           UNIQUE (user_id, product_id)
);
