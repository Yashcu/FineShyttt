package com.yash.fineshyttt.repository;

import com.yash.fineshyttt.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    // Find active coupons that are currently valid
    Optional<Coupon> findByCodeAndIsActiveTrueAndValidFromBeforeAndValidUntilAfter(
            String code,
            Instant now1,
            Instant now2
    );
}
