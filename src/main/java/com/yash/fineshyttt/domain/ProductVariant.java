package com.yash.fineshyttt.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String material;

    @Column(nullable = false)
    private String color;

    @Column
    private String size;

    @Column(nullable = false)
    private BigDecimal price;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToOne(mappedBy = "variant", fetch = FetchType.LAZY, optional = false)
    private Inventory inventory;

    public ProductVariant(
            Product product,
            String sku,
            String material,
            String color,
            String size,
            BigDecimal price
    ) {
        this.product = product;
        this.sku = sku;
        this.material = material;
        this.color = color;
        this.size = size;
        this.price = price;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
