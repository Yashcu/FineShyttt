package com.yash.fineshyttt.service.auth;

import com.yash.fineshyttt.domain.RefreshToken;
import com.yash.fineshyttt.domain.User;
import com.yash.fineshyttt.repository.RefreshTokenRepository;
import com.yash.fineshyttt.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yash.fineshyttt.exception.AuthenticationException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Duration REUSE_GRACE = Duration.ofSeconds(30);

    private final RefreshTokenRepository repository;
    private final TokenHasher tokenHasher;

    public RefreshTokenResult create(User user, String deviceFingerprint){
        String rawToken = UUID.randomUUID().toString();
        Instant now = Instant.now();

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHasher.hash(rawToken))
                .deviceFingerprint(deviceFingerprint)
                .issuedAt(now)
                .expiresAt(now.plus(REFRESH_TTL))
                .revoked(false)
                .build();

        repository.save(token);

        return new RefreshTokenResult(rawToken, token.getExpiresAt());
    }

    public User validateAndRotate(String rawToken){
        String hash = tokenHasher.hash(rawToken);
        RefreshToken token = repository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        Instant now = Instant.now();

        if (token.isRevoked()) {
            handleReuseAttack(token, now);
        }

        if (token.getExpiresAt().isBefore(now)) {
            throw new AuthenticationException("Refresh token expired");
        }

        token.setRevoked(true);
        token.setRevokedAt(now);
        token.setLastUsedAt(now);

        return token.getUser();
    }

    private void handleReuseAttack(RefreshToken token, Instant now) {
        if (token.getLastUsedAt() != null &&
                Duration.between(token.getLastUsedAt(), now).compareTo(REUSE_GRACE) <= 0) {
            return; // grace window
        }

        repository.revokeAllByUserId(token.getUser().getId(), now);
        throw new AuthenticationException("Refresh token reuse detected");
    }

    public void revokeAllForUser(Long userId) {
        repository.revokeAllByUserId(userId, Instant.now());
    }
}
