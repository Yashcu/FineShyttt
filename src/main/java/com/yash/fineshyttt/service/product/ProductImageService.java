package com.yash.fineshyttt.service.product;

import com.yash.fineshyttt.domain.Product;
import com.yash.fineshyttt.domain.ProductImage;
import com.yash.fineshyttt.exception.ResourceNotFoundException;
import com.yash.fineshyttt.repository.ProductImageRepository;
import com.yash.fineshyttt.repository.ProductRepository;
import com.yash.fineshyttt.service.media.MediaStorageService;
import com.yash.fineshyttt.service.media.PresignedUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final MediaStorageService storageService;

    public PresignedUpload requestUpload(
            Long productId,
            String contentType
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow();

        String objectKey =
                "products/" + product.getId() + "/" + System.nanoTime();

        return storageService.generatePresignedUrl(
                objectKey,
                contentType
        );
    }

    public void confirmUpload(
            Long productId,
            String imageUrl,
            int position,
            boolean primary
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with ID: " + productId)
                );

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .position(position)
                .isPrimary(primary)
                .build();

        imageRepository.save(image);
    }
}
