package com.yash.fineshyttt.service.product;

import com.yash.fineshyttt.domain.Product;
import com.yash.fineshyttt.exception.ResourceNotFoundException;
import com.yash.fineshyttt.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductRepository productRepository;

    public Page<Product> getActiveProducts(Pageable pageable) {
        return productRepository.findAllByIsActiveTrue(pageable);
    }

    public Page<Product> getActiveProductsByCategory(
            Long categoryId,
            Pageable pageable
    ) {
        return productRepository.findAllByCategory_IdAndIsActiveTrue(
                categoryId,
                pageable
        );
    }

    public Product getBySlug(String slug) {
        return productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );
    }
}
