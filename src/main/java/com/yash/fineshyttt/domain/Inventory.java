package com.yash.fineshyttt.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "variant_id", unique = true)
    private ProductVariant variant;

    @Column(nullable = false)
    private int quantity;

    @Builder.Default
    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    // Business methods...
    public void increase(int amount) {
        this.quantity += amount;
        this.updatedAt = Instant.now();
    }

    public void decrease(int amount) {
        if (this.quantity < amount) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.quantity -= amount;
        this.updatedAt = Instant.now();
    }

    public void reserve(int amount) {
        if (this.quantity - this.reservedQuantity < amount) {
            throw new IllegalStateException("Insufficient available stock");
        }
        this.reservedQuantity += amount;
        this.updatedAt = Instant.now();
    }

    public void releaseReservation(int amount) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - amount);
        this.updatedAt = Instant.now();
    }

    public int getAvailableQuantity() {
        return this.quantity - this.reservedQuantity;
    }
}
