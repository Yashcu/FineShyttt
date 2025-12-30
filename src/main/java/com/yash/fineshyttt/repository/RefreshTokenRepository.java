package com.yash.fineshyttt.repository;

import com.yash.fineshyttt.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revoked = true, rt.revokedAt = :now
        WHERE rt.user.id = :userId AND rt.revoked = false
    """)
    void revokeAllByUserId(@Param("userId") Long userId,
                          @Param("now") Instant now);
}
