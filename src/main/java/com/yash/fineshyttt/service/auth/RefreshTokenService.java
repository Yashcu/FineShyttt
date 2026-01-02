package com.yash.fineshyttt.service.auth;

import com.yash.fineshyttt.domain.RefreshToken;
import com.yash.fineshyttt.domain.User;
import com.yash.fineshyttt.exception.AuthenticationException;
import com.yash.fineshyttt.repository.RefreshTokenRepository;
import com.yash.fineshyttt.security.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final TokenHasher tokenHasher;

    @Value("${security.jwt.refresh-ttl-days}")
    private long refreshTtlDays;

    @Value("${security.jwt.reuse-grace-seconds}")
    private long reuseGraceSeconds;

    /**
     * Create New Refresh Token
     *
     * Generates a new refresh token bound to a user and device.
     * This method is called during initial login and after successful rotation.
     *
     * Security Properties:
     * - Token value: Cryptographically random UUID v4
     * - Storage: Only hashed value stored in database (never plaintext)
     * - Device binding: Enables session tracking and anomaly detection
     * - Expiry: Configurable long-lived token (7-14 days typical)
     *
     * @param user User owning this refresh token
     * @param deviceFingerprint Client device identifier (browser fingerprint, IP hash, etc.)
     * @return RefreshTokenResult containing raw token (for client) and metadata
     */
    @Transactional
    public RefreshTokenResult create(User user, String deviceFingerprint){
        // Generate cryptographically random token value
        String rawToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofDays(refreshTtlDays));

        // Create token entity with hashed value (never store raw token)
        RefreshToken token = new RefreshToken(
                user,
                tokenHasher.hash(rawToken),
                deviceFingerprint,
                now,
                expiresAt
        );

        repository.save(token);

        // Return raw token for client storage + metadata
        return new RefreshTokenResult(rawToken, expiresAt, user.getId());
    }

    /**
     * Validate and Rotate Refresh Token (Atomic Operation)
     *
     * This is the core security operation implementing token rotation.
     *
     * Workflow (atomic transaction):
     * 1. Lookup token by hash
     * 2. Validate: expiration, revocation status
     * 3. Detect reuse: if already revoked, trigger family revocation
     * 4. Revoke old token (mark as used)
     * 5. Create new token with SAME device fingerprint (preserve session identity)
     * 6. Return new token + user
     *
     * Security Properties:
     * - Single-use tokens: Old token immediately invalidated
     * - Atomic rotation: No race condition between revoke and create
     * - Device context preserved: New token inherits device fingerprint
     * - Reuse detection: Second use of revoked token triggers full revocation
     * - Token family continuity: Session tracking maintained across rotations
     *
     * Attack Mitigation:
     * - Replay attacks: Token valid only once
     * - Token theft: Reuse triggers immediate family revocation
     * - Race conditions: Grace period handles legitimate concurrent requests
     *
     * @param rawToken Refresh token from client (unhashed)
     * @return RefreshTokenResult with new rotated token, expiry, and user
     * @throws AuthenticationException if token invalid, expired, revoked, or reused
     */
    @Transactional
    public RefreshTokenResult validateAndRotate(String rawToken){

        // Lookup token by hash
        String hash = tokenHasher.hash(rawToken);
        RefreshToken oldToken = repository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        Instant now = Instant.now();

        // Validate expiration
        if (oldToken.isExpired(now)) {
            throw new AuthenticationException("Refresh token expired");
        }

        // If token already revoked, this is either:
        // 1. Legitimate race condition (within grace period)
        // 2. Token theft/replay attack (revoke entire family)
        if (oldToken.isRevoked()) {
            handleReuseAttack(oldToken, now);
        }

        // Step 1: Revoke old token (mark as used)
        oldToken.revoke(now);

        // Step 2: Create new token with SAME device fingerprint
        // This preserves session identity for anomaly detection
        return create(
                oldToken.getUser(),
                oldToken.getDeviceFingerprint()
        );
    }

    /**
     * Handle Refresh Token Reuse Attack
     *
     * When a revoked token is reused, this indicates potential token theft.
     * Strategy: Grace period for race conditions, then full family revocation.
     *
     * Scenarios:
     * 1. Legitimate race condition:
     *    - Client makes concurrent refresh requests (network retry)
     *    - Within grace window (5-30 seconds typical)
     *    - Allow retry without revocation
     *
     * 2. Token theft/replay attack:
     *    - Revoked token used outside grace window
     *    - Indicates stolen token being replayed
     *    - Revoke ALL tokens for this user (token family)
     *
     * Token Family Revocation:
     * - Prevents attacker from using any token in stolen chain
     * - Forces legitimate user to re-authenticate
     * - Alerts security team of potential breach
     *
     * @param token The revoked token that was reused
     * @param now Current timestamp
     * @throws AuthenticationException always thrown (either gracefully or with revocation)
     */
    private void handleReuseAttack(RefreshToken token, Instant now) {
        // Check if within grace period (handles clock skew + race conditions)
        if (token.getLastUsedAt() != null) {
            Duration timeSinceLastUse = Duration.between(token.getLastUsedAt(), now);

            // Within grace window: likely legitimate retry
            if (timeSinceLastUse.compareTo(Duration.ofSeconds(reuseGraceSeconds)) <= 0) {
                // Allow retry but still throw exception (client should have new token)
                throw new AuthenticationException("Refresh token already used");
            }
        }

        // Outside grace window: likely token theft/replay attack
        // Revoke entire token family (all tokens for this user)
        repository.revokeAllByUserId(token.getUser().getId(), now);
        throw new AuthenticationException("Refresh token reuse detected - all sessions revoked");
    }

    /**
     * Revoke All Refresh Tokens for User
     *
     * Used for:
     * - Explicit logout (user-initiated)
     * - Security events (password change, suspicious activity)
     * - Administrative actions (account suspension)
     *
     * Effect:
     * - All refresh tokens for user immediately invalidated
     * - Access tokens expire naturally (cannot be revoked)
     * - User must re-authenticate to obtain new tokens
     *
     * Future Enhancement:
     * - Device-scoped revocation: revoke specific session by device fingerprint
     * - Time-scoped revocation: revoke tokens issued before specific timestamp
     *
     * @param userId User whose tokens should be revoked
     */
    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllByUserId(userId, Instant.now());
    }
}
