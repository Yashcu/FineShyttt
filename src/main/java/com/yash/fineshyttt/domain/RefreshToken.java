package com.yash.fineshyttt.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    private Instant revokedAt;
    private Instant lastUsedAt;

    public RefreshToken(
            User user,
            String tokenHash,
            String deviceFingerprint,
            Instant issuedAt,
            Instant expiresAt
    ) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.deviceFingerprint = deviceFingerprint;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public void revoke(Instant now) {
        this.revoked = true;
        this.revokedAt = now;
        this.lastUsedAt = now;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

}
