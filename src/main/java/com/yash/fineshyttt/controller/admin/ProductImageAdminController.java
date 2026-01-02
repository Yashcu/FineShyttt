package com.yash.fineshyttt.controller.admin;

import com.yash.fineshyttt.service.media.PresignedUpload;
import com.yash.fineshyttt.service.product.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products/{productId}/images")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductImageAdminController {

    private final ProductImageService imageService;

    @PostMapping("/upload-url")
    public ResponseEntity<PresignedUpload> requestUpload(
            @PathVariable Long productId,
            @RequestParam String contentType
    ) {
        return ResponseEntity.ok(
                imageService.requestUpload(productId, contentType)
        );
    }

    @PostMapping
    public ResponseEntity<Void> confirmUpload(
            @PathVariable Long productId,
            @RequestParam String imageUrl,
            @RequestParam int position,
            @RequestParam boolean primary
    ) {
        imageService.confirmUpload(
                productId,
                imageUrl,
                position,
                primary
        );
        return ResponseEntity.noContent().build();
    }
}
