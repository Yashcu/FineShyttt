package com.yash.fineshyttt.dto.product;

import com.yash.fineshyttt.domain.Product;
import com.yash.fineshyttt.domain.ProductImage;

import java.util.Comparator;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String slug,
        String description,
        Long categoryId,
        boolean active,
        List<String> images
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getCategory().getId(),
                product.isActive(),
                product.getImages()
                        .stream()
                        .sorted(Comparator.comparingInt(ProductImage::getPosition))
                        .map(ProductImage::getImageUrl)
                        .toList()
        );
    }
}

