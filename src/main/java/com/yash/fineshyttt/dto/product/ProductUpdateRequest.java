package com.yash.fineshyttt.dto.product;

import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @Size(min = 1, max = 255, message = "Name must be 1-255 characters")
        String name,

        @Size(min = 1, max = 5000, message = "Description must be 1-5000 characters")
        String description,

        Long categoryId,
        Boolean isActive
) {}
