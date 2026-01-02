package com.yash.fineshyttt.controller.publicapi;

import com.yash.fineshyttt.dto.product.ProductResponse;
import com.yash.fineshyttt.service.product.ProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductPublicController {

    private final ProductQueryService queryService;

    @GetMapping
    public Page<ProductResponse> getAll(Pageable pageable) {
        return queryService.getActiveProducts(pageable)
                .map(ProductResponse::from);
    }

    @GetMapping("/category/{categoryId}")
    public Page<ProductResponse> getByCategory(
            @PathVariable Long categoryId,
            Pageable pageable
    ) {
        return queryService
                .getActiveProductsByCategory(categoryId, pageable)
                .map(ProductResponse::from);
    }

    @GetMapping("/{slug}")
    public ProductResponse getBySlug(@PathVariable String slug) {
        return ProductResponse.from(
                queryService.getBySlug(slug)
        );
    }
}
