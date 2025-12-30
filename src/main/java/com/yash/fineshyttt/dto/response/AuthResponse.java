package com.yash.fineshyttt.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
