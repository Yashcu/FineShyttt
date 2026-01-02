package com.yash.fineshyttt.security;

import com.yash.fineshyttt.domain.Role;
import com.yash.fineshyttt.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Service - RS256 Implementation
 *
 * Generates and validates JWT access tokens using RSA asymmetric keys.
 *
 * Security Properties:
 * - RS256 (RSA + SHA-256) signature algorithm
 * - Private key for signing (kept secret)
 * - Public key for validation (can be distributed)
 * - Short-lived tokens (15 minutes)
 * - Self-contained (no database lookup for validation)
 *
 * Key Management:
 * - Private key: Never commit to VCS, load from secure storage
 * - Public key: Can be shared with other services (future microservices)
 * - Keys loaded at startup and cached in memory
 *
 * Upgrade from HS256:
 * - HS256 uses symmetric key (same key signs and validates)
 * - RS256 uses asymmetric keys (private signs, public validates)
 * - If public key leaks, attacker still cannot forge tokens
 */
@Service
@Slf4j
public class JwtService {

    @Value("${security.jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${security.jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${security.jwt.access-ttl-minutes:15}")
    private long accessTtlMinutes;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * Initialize RSA Keys at Startup
     *
     * Loads private and public keys from PEM files.
     * Validates keys are present and loadable.
     * Fails fast if keys missing or corrupted.
     *
     * Security Note:
     * - Private key must be kept secret (never commit to git)
     * - Public key can be shared (used only for validation)
     * - Keys should be rotated periodically (every 6-12 months)
     *
     * @throws IllegalStateException if keys cannot be loaded
     */
    @PostConstruct
    void init() {
        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);

            log.info("JWT Service initialized with RS256 keys");
            log.debug("Private key algorithm: {}", privateKey.getAlgorithm());
            log.debug("Public key algorithm: {}", publicKey.getAlgorithm());

        } catch (Exception ex) {
            log.error("Failed to load JWT keys", ex);
            throw new IllegalStateException("JWT keys initialization failed", ex);
        }
    }

    /**
     * Generate Access Token (RS256)
     *
     * Creates a short-lived JWT access token signed with RSA private key.
     * Token contains user identity, roles, and metadata for authorization.
     *
     * Standard Claims:
     * - sub (Subject): User ID (primary identity claim)
     * - jti (JWT ID): Unique token identifier (for audit/revocation tracking)
     * - iat (Issued At): Token creation timestamp
     * - exp (Expiration): Token expiration timestamp (enforced by parser)
     *
     * Custom Claims:
     * - email: User's email address (convenience, non-sensitive)
     * - roles: Array of role names (RBAC authorization)
     * - type: Token type discriminator ("ACCESS" vs future "REFRESH_JWT")
     *
     * Security Properties:
     * - Signed with RS256 (prevents tampering)
     * - Short-lived (reduces impact of token theft)
     * - Self-contained (no database lookup needed)
     * - Role-based (enables fine-grained authorization)
     *
     * @param user User entity (must have ID, email, and roles loaded)
     * @return Signed JWT token string (format: header.payload.signature)
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTtlMinutes * 60);

        String token = Jwts.builder()
                .setSubject(user.getId().toString()) // Primary identity
                .setId(UUID.randomUUID().toString()) // Unique token ID (jti)
                .setIssuedAt(Date.from(now)) // Token creation time
                .setExpiration(Date.from(expiresAt)) // Token expiration
                .claim("email", user.getEmail()) // User email (convenience)
                .claim("roles", user.getRoles() // RBAC roles
                        .stream()
                        .map(Role::getName)
                        .toList())
                .claim("type", "ACCESS")  // Token type discriminator
                .signWith(privateKey, SignatureAlgorithm.RS256) // RS256 signature
                .compact();

        log.debug("Generated access token for userId={}, expiresAt={}",
                user.getId(), expiresAt);

        return token;
    }

    /**
     * Parse and Validate Token (RS256)
     *
     * Verifies JWT signature using RSA public key and extracts claims.
     * Automatically enforces expiration and signature validity.
     *
     * Validation Checks (automatic):
     * ✅ Signature verification (prevents tampering)
     * ✅ Expiration check (rejects expired tokens)
     * ✅ Clock skew tolerance (30 seconds for distributed systems)
     *
     * Security Properties:
     * - Uses public key (no secret exposed)
     * - Constant-time validation (prevents timing attacks)
     * - Fails fast on invalid tokens
     *
     * @param token JWT token string (format: header.payload.signature)
     * @return Claims object containing all token claims
     * @throws io.jsonwebtoken.JwtException if token invalid, expired, or tampered
     */
    public Claims parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey) // Use public key for validation
                    .setAllowedClockSkewSeconds(30) // Handle clock drift
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.debug("Token validated successfully: userId={}", claims.getSubject());
            return claims;

        } catch (JwtException ex) {
            log.debug("Token validation failed: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Extract User ID from Token
     *
     * Convenience method to extract user identity (sub claim).
     * Used by authentication filters to load UserPrincipal.
     *
     * @param token JWT token string
     * @return User's database ID
     * @throws io.jsonwebtoken.JwtException if token invalid
     */
    public Long extractUserId(String token) {
        return Long.parseLong(parseAndValidate(token).getSubject());
    }

    /**
     * Extract Email from Token
     *
     * Convenience method to extract user email (custom claim).
     * Useful for logging/audit without database lookup.
     *
     * @param token JWT token string
     * @return User's email address
     * @throws io.jsonwebtoken.JwtException if token invalid
     */
    public String extractEmail(String token) {
        return parseAndValidate(token).get("email", String.class);
    }

    /**
     * Validate Token Type (Access Token Only)
     *
     * Ensures token is an access token, not a refresh token or other type.
     * Prevents token type confusion attacks.
     *
     * Future Enhancement:
     * - Support JWT-based refresh tokens (currently using opaque tokens)
     * - Validate token audience (aud claim)
     * - Validate token issuer (iss claim)
     *
     * @param claims Parsed JWT claims
     * @throws JwtException if token type is not ACCESS
     */
    public void validateAccessToken(Claims claims) {
        String type = claims.get("type", String.class);
        if (!"ACCESS".equals(type)) {
            log.warn("Invalid token type: expected=ACCESS, actual={}", type);
            throw new JwtException("Invalid token type: " + type);
        }
    }

    /**
     * Load RSA Private Key from PEM File
     *
     * Reads PKCS#8 formatted private key from classpath.
     * Strips PEM headers and decodes Base64 content.
     *
     * PEM Format:
     * -----BEGIN PRIVATE KEY-----
     * Base64EncodedKey
     * -----END PRIVATE KEY-----
     *
     * Security Notes:
     * - Private key must NEVER be committed to version control
     * - Store in secure location (environment variable, secrets manager, etc.)
     * - Rotate keys periodically (every 6-12 months)
     *
     * @param path Classpath resource path (e.g., "classpath:keys/jwt-private.pem")
     * @return Loaded RSA private key
     * @throws Exception if key file not found or invalid format
     */
    private PrivateKey loadPrivateKey(String path) throws Exception {
        log.debug("Loading private key from: {}", path);

        String keyContent = new String(Files.readAllBytes(
                ResourceUtils.getFile(path).toPath()))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", ""); // Remove all whitespace

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        log.debug("Private key loaded successfully");
        return kf.generatePrivate(spec);
    }

    /**
     * Load RSA Public Key from PEM File
     *
     * Reads X.509 formatted public key from classpath.
     * Strips PEM headers and decodes Base64 content.
     *
     * PEM Format:
     * -----BEGIN PUBLIC KEY-----
     * Base64EncodedKey
     * -----END PUBLIC KEY-----
     *
     * Security Notes:
     * - Public key can be shared safely (used only for validation)
     * - Can be distributed to other services in microservices architecture
     * - Should match private key (verify key pair before deployment)
     *
     * @param path Classpath resource path (e.g., "classpath:keys/jwt-public.pem")
     * @return Loaded RSA public key
     * @throws Exception if key file not found or invalid format
     */
    private PublicKey loadPublicKey(String path) throws Exception {
        log.debug("Loading public key from: {}", path);

        String keyContent = new String(Files.readAllBytes(
                ResourceUtils.getFile(path).toPath()))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", ""); // Remove all whitespace

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        log.debug("Public key loaded successfully");
        return kf.generatePublic(spec);
    }
}
