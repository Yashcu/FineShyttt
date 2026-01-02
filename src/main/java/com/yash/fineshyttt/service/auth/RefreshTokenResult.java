package com.yash.fineshyttt.service.auth;

import java.time.Instant;

public record RefreshTokenResult (
        String rawValue,
        Instant expiresAt,
        Long userId
) {}
