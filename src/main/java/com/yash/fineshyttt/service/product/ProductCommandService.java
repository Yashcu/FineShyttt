package com.yash.fineshyttt.service.product;

import com.yash.fineshyttt.domain.Category;
import com.yash.fineshyttt.domain.Product;
import com.yash.fineshyttt.exception.ResourceNotFoundException;
import com.yash.fineshyttt.repository.CategoryRepository;
import com.yash.fineshyttt.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SlugService slugService;

    public Product createProduct(
            String name,
            String description,
            Long categoryId
    ) {
        validateName(name);
        validateDescription(description);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found")
                );

        String uniqueSlug = slugService.generateUnique(name);

        Product product = Product.builder()
                .name(name)
                .slug(uniqueSlug)
                .description(description)
                .category(category)
                .isActive(true)
                .build();

        return productRepository.save(product);
    }

    public Product updateProduct(
            Long productId,
            String name,
            String description,
            Long categoryId,
            Boolean isActive,
            boolean regenerateSlug
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

        if (name != null && !name.isBlank()) {
            product.setName(name);

            if (regenerateSlug) {
                product.setSlug(slugService.generateUnique(name));
            }
        }

        if (description != null) {
            validateDescription(description);
            product.setDescription(description);
        }

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Category not found")
                    );
            product.setCategory(category);
        }

        if (isActive != null) {
            product.setActive(isActive);
        }

        return productRepository.save(product);
    }

    // -------------------------
    // Guards
    // -------------------------

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Product description cannot be empty");
        }
    }
}
