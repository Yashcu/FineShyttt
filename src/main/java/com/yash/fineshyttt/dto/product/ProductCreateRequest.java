package com.yash.fineshyttt.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull Long categoryId
) {}
