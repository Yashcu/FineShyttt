package com.yash.fineshyttt.controller.auth;

import com.yash.fineshyttt.config.ApiConstants;
import com.yash.fineshyttt.dto.auth.AuthResponse;
import com.yash.fineshyttt.dto.auth.LoginRequest;
import com.yash.fineshyttt.dto.auth.RefreshRequest;
import com.yash.fineshyttt.security.UserPrincipal;
import com.yash.fineshyttt.service.UserService;
import com.yash.fineshyttt.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.yash.fineshyttt.dto.auth.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.AUTH_BASE) // CHANGED: Use constant
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * User Login
     *
     * Authenticates user credentials and issues JWT access + refresh tokens.
     * Device fingerprint is captured for session tracking and security.
     *
     * @param request Contains email, password, and optional device fingerprint
     * @return AuthResponse with access token, refresh token, and token metadata
     */
    @PostMapping(ApiConstants.AUTH_LOGIN) // CHANGED: Use constant
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                authService.login(
                        request.email(),
                        request.password(),
                        request.deviceFingerprint()
                )
        );
    }

    /**
     * User Registration
     *
     * Creates a new customer account with default CUSTOMER role.
     * Role assignment is enforced in service layer, not controller.
     *
     * @param request Contains email and password (validated via @Valid)
     * @return 201 Created with no response body
     */
    @PostMapping(ApiConstants.AUTH_REGISTER) // CHANGED: Use constant
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.registerCustomer(
                request.email(),
                request.password()
        );
        return ResponseEntity.status(201).build();
    }

    /**
     * Token Refresh
     *
     * Rotates refresh token and issues new access token.
     * Implements refresh token rotation for enhanced security.
     * Old refresh token is invalidated after successful rotation.
     *
     * @param request Contains current refresh token
     * @return AuthResponse with new access token and rotated refresh token
     */
    @PostMapping(ApiConstants.AUTH_REFRESH) // CHANGED: Use constant
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(
                authService.refresh(request.refreshToken())
        );
    }

    /**
     * User Logout
     *
     * Revokes all refresh tokens for the authenticated user.
     * Access token expires naturally (cannot be revoked server-side).
     * User identity sourced from SecurityContext, never from request payload.
     *
     * @param principal Authenticated user from Spring Security context
     * @return 204 No Content
     */
    @PostMapping(ApiConstants.AUTH_LOGOUT) // CHANGED: Use constant
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authService.logout(principal);
        return ResponseEntity.noContent().build();
    }
}
