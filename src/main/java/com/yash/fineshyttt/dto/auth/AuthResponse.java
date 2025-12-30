package com.yash.fineshyttt.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
