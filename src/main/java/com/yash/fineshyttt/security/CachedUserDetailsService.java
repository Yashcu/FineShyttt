package com.yash.fineshyttt.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Cached User Details Service
 *
 * Adds in-memory caching layer on top of UserDetailsServiceImpl to reduce database load.
 *
 * Problem:
 * - JwtAuthenticationFilter calls UserDetailsService on EVERY authenticated request
 * - Database hit for user + roles fetch adds 5-20ms latency per request
 * - At 100 req/sec, that's 100 DB queries/sec just for user loading
 *
 * Solution:
 * - Cache UserPrincipal objects in memory for 5 minutes
 * - Cache hit = 0ms (no database), Cache miss = 10ms (database lookup)
 * - Expected cache hit rate: 95%+ after warm-up
 *
 * Cache Configuration:
 * - TTL: 5 minutes (balances staleness vs performance)
 * - Max size: 10,000 users (prevents memory exhaustion)
 * - Eviction: LRU (least recently used)
 *
 * Cache Invalidation:
 * - Manual: Call invalidate() when user data changes
 * - Automatic: TTL expiration after 5 minutes
 *
 * Trade-offs:
 * ✅ Pros: 95%+ reduction in DB load, 10-20ms faster response times
 * ⚠️ Cons: Stale data for up to 5 minutes (acceptable for user/role data)
 *
 * Future Enhancements:
 * - Use Redis for distributed cache (multi-instance deployments)
 * - Add cache metrics endpoint (hit rate, evictions, etc.)
 * - Invalidate cache on user update events (Spring Events)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachedUserDetailsService {

    private final UserDetailsServiceImpl delegate;

    /**
     * Caffeine In-Memory Cache
     *
     * Configuration:
     * - expireAfterWrite: 5 minutes (fresh enough for user/role data)
     * - maximumSize: 10,000 users (prevents memory exhaustion)
     * - recordStats: Enables cache hit/miss metrics
     *
     * Memory Usage:
     * - UserPrincipal size: ~500 bytes (user + roles)
     * - 10,000 users = ~5 MB memory (negligible)
     */
    private final Cache<Long, UserPrincipal> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES) // Auto-expire after 5 min
            .maximumSize(10_000) // Max 10k users cached
            .recordStats() // Enable metrics
            .build();

    /**
     * Load User by ID (with caching)
     *
     * Workflow:
     * 1. Check cache for userId
     * 2. If cache hit: Return cached UserPrincipal (0ms)
     * 3. If cache miss: Load from database via delegate (10ms)
     * 4. Store result in cache for future requests
     *
     * Performance:
     * - Cache hit: ~0ms (in-memory lookup)
     * - Cache miss: ~10ms (DB query + cache store)
     * - Expected hit rate: 95%+ after warm-up
     *
     * Thread Safety:
     * - Caffeine cache is thread-safe (concurrent reads/writes)
     * - Lambda function is atomic (no race conditions)
     *
     * @param userId User's database ID (from JWT sub claim)
     * @return UserPrincipal with user entity and roles
     */
    public UserDetails loadUserById(Long userId) {
        UserPrincipal userPrincipal = cache.get(userId, id -> {
            log.debug("Cache MISS for userId={}, loading from database", id);
            return (UserPrincipal) delegate.loadUserById(id);
        });

        log.trace("Cache HIT for userId={}", userId);
        return userPrincipal;
    }

    /**
     * Invalidate Cache for Specific User
     *
     * Use Cases:
     * - User updates profile (name, email, etc.)
     * - User changes password (force re-authentication)
     * - User roles changed (permission update)
     * - User account disabled/enabled
     *
     * Call this method after any user modification to ensure cache consistency.
     *
     * @param userId User ID to invalidate
     */
    public void invalidate(Long userId) {
        cache.invalidate(userId);
        log.debug("Cache invalidated for userId={}", userId);
    }

    /**
     * Invalidate Entire Cache
     *
     * Use Cases:
     * - System-wide role permission changes
     * - Deployment rollback (reset all cached state)
     * - Manual admin action (debugging)
     *
     * WARNING: Causes temporary performance degradation until cache warms up.
     * Use sparingly in production.
     */
    public void invalidateAll() {
        cache.invalidateAll();
        log.warn("Entire user cache invalidated");
    }

    /**
     * Get Cache Statistics
     *
     * Metrics:
     * - hitRate: Percentage of cache hits (target: >95%)
     * - missRate: Percentage of cache misses
     * - evictionCount: Number of entries evicted (LRU)
     * - loadSuccessCount: Successful DB loads
     *
     * Usage:
     * - Expose via actuator endpoint for monitoring
     * - Log periodically to track cache effectiveness
     *
     * @return CacheStats with hit rate, miss rate, evictions, etc.
     */
    public CacheStats getStats() {
        CacheStats stats = cache.stats();
        log.info("Cache stats: hitRate={}, missRate={}, evictionCount={}, size={}",
                stats.hitRate(), stats.missRate(), stats.evictionCount(), cache.estimatedSize());
        return stats;
    }

    /**
     * Get Current Cache Size
     *
     * Returns approximate number of cached entries.
     * Useful for monitoring memory usage.
     *
     * @return Estimated cache size (may be slightly inaccurate due to concurrency)
     */
    public long getCacheSize() {
        return cache.estimatedSize();
    }
}
