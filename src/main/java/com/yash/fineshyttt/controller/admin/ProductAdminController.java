package com.yash.fineshyttt.controller.admin;

import com.yash.fineshyttt.dto.product.ProductCreateRequest;
import com.yash.fineshyttt.dto.product.ProductResponse;
import com.yash.fineshyttt.dto.product.ProductUpdateRequest;
import com.yash.fineshyttt.service.product.ProductCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductAdminController {

    private final ProductCommandService commandService;

    @PostMapping
    public ProductResponse create(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        return ProductResponse.from(
                commandService.createProduct(
                        request.name(),
                        request.description(),
                        request.categoryId()
                )
        );
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ProductResponse.from(
                commandService.updateProduct(
                        id,
                        request.name(),
                        request.description(),
                        request.categoryId(),
                        request.isActive(),
                        false
                )
        );
    }
}
