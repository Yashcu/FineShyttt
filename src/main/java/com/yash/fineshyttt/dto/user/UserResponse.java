package com.yash.fineshyttt.dto.user;

import java.time.Instant;
import java.util.List;

/**
 * User Response DTO
 *
 * Public representation of User entity.
 * Excludes sensitive fields (passwordHash, etc.).
 *
 * Used by:
 * - GET /api/v1/users/me (user profile)
 * - GET /api/v1/admin/users/{id} (admin view)
 */
public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        boolean emailVerified,
        boolean enabled,
        Instant lastLoginAt,
        List<String> roles, // Role names only (not full Role entities)
        Instant createdAt
) {}
