package com.yash.fineshyttt.dto.product;

public record ProductImageResponse(
        String url,
        boolean primary,
        int position
) {}
