package com.yash.fineshyttt.service.auth;

import com.yash.fineshyttt.domain.User;
import com.yash.fineshyttt.dto.auth.AuthResponse;
import com.yash.fineshyttt.exception.AuthenticationException;
import com.yash.fineshyttt.security.JwtService;
import com.yash.fineshyttt.security.UserPrincipal;
import com.yash.fineshyttt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate User and Issue Token Pair
     * <p>
     * Workflow:
     * 1. Lookup user by email
     * 2. Validate account status (enabled/disabled)
     * 3. Verify password hash
     * 4. Update login timestamp
     * 5. Generate stateless access token (JWT)
     * 6. Create stateful refresh token with device binding
     * <p>
     * Security Notes:
     * - Password comparison uses constant-time algorithm
     * - Device fingerprint enables session tracking and anomaly detection
     * - Failed authentication throws generic error to prevent user enumeration
     *
     * @param email             User's email address
     * @param password          Plain-text password from request
     * @param deviceFingerprint Client device identifier (browser fingerprint, IP, user-agent hash)
     * @return AuthResponse containing access token and refresh token
     * @throws AuthenticationException if credentials invalid or account disabled
     */
    @Transactional
    public AuthResponse login(String email, String password, String deviceFingerprint) {

        // Lookup user - throws exception if not found
        User user = userService.findByEmail(email);

        // Check account status before password verification
        if (!user.isEnabled()) {
            throw new AuthenticationException("Account disabled");
        }

        // Constant-time password comparison to prevent timing attacks
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        // Update last_login_at timestamp (domain logic)
        user.recordLogin();

        // Generate stateless JWT access token (short-lived)
        String accessToken = jwtService.generateAccessToken(user);

        // Create stateful refresh token with device binding
        RefreshTokenResult refresh = refreshTokenService.create(user, deviceFingerprint);

        return new AuthResponse(accessToken, refresh.rawValue());
    }

    /**
     * Refresh Access Token with Refresh Token Rotation
     *
     * Workflow:
     * 1. Validate incoming refresh token (signature, expiration, revocation status)
     * 2. Rotate refresh token (invalidate old, create new with same device context)
     * 3. Detect reuse attempts (security violation if old token reused)
     * 4. Generate new access token
     * 5. Return new token pair
     *
     * Security Model:
     * - Single-use refresh tokens: old token immediately invalidated after use
     * - Reuse detection: if old token used again, entire token family revoked
     * - Device context preserved: rotation maintains session/device binding
     * - Token family tracking: enables detection of stolen token chains
     *
     * Critical Security Property:
     * Rotation MUST preserve device/session identity, not create new sessions.
     * This enables anomaly detection (location hopping, device switching).
     *
     * @param rawRefreshToken Refresh token from client (unhashed)
     * @return AuthResponse with new access token and rotated refresh token
     * @throws AuthenticationException if token invalid, expired, revoked, or reused
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        // This ensures atomic validation, rotation, and reuse detection
        RefreshTokenResult rotatedToken = refreshTokenService.validateAndRotate(rawRefreshToken);

        // Extract user from validated token result
        User user = userService.findById(rotatedToken.userId());

        // Generate new stateless access token
        String accessToken = jwtService.generateAccessToken(user);

        return new AuthResponse(
                accessToken,
                rotatedToken.rawValue() // New rotated refresh token
        );

    }

    /**
     * Logout User by Revoking All Refresh Tokens
     *
     * Behavior:
     * - Revokes ALL refresh tokens for the authenticated user
     * - Access tokens expire naturally (cannot be revoked server-side)
     * - User must re-authenticate to obtain new tokens
     *
     * Security Notes:
     * - User identity sourced from SecurityContext, never from request payload
     * - Prevents token theft aftermath (stolen tokens become useless)
     * - Supports "logout from all devices" functionality
     *
     * Future Enhancement:
     * - Device-scoped logout: revoke tokens for current device only
     * - Session-scoped logout: revoke specific session by device fingerprint
     *
     * @param principal Authenticated user from Spring Security context
     */
    @Transactional
    public void logout(UserPrincipal principal) {
        // Revoke all refresh tokens for this user across all devices/sessions
        refreshTokenService.revokeAllForUser(principal.userId());
    }
}
