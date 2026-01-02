package com.yash.fineshyttt.security;

import com.yash.fineshyttt.config.ApiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Spring Security Configuration
 *
 * Configures:
 * - Authentication (JWT-based, stateless)
 * - Authorization (role-based access control)
 * - Security headers (HSTS, CSP, X-Frame-Options, etc.)
 * - Filter chain (JWT filter before default authentication)
 *
 * Security Model:
 * - Stateless (no HTTP sessions)
 * - JWT-based authentication (access tokens)
 * - RBAC (role-based authorization)
 * - Fail-safe defaults (authenticated by default)
 *
 * Aligned with:
 * - SECURITY_MODEL.md (JWT, RBAC, security headers)
 * - API_CONTRACTS.md (public/admin route separation)
 * - Architecture.md (stateless, layered security)
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // Enables @PreAuthorize, @Secured, @RolesAllowed
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    /**
     * Authentication Manager Bean
     *
     * Exposes Spring's AuthenticationManager for use in services (e.g., AuthService).
     * Delegates to configured authentication providers (DaoAuthenticationProvider).
     *
     * Used by:
     * - AuthService.login() to validate credentials
     *
     * @param authConfig Spring's authentication configuration
     * @return Configured authentication manager
     * @throws Exception if authentication manager cannot be created
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Security Filter Chain
     *
     * Configures the complete security policy for HTTP requests.
     *
     * Configuration:
     * 1. CSRF disabled (JWT-based, no cookies)
     * 2. CORS enabled (cross-origin requests allowed)
     * 3. Stateless sessions (no server-side session storage)
     * 4. Security headers (HSTS, CSP, X-Frame-Options, etc.)
     * 5. Authorization rules (public/admin/authenticated routes)
     * 6. JWT filter (runs before default authentication)
     *
     * Route Security Matrix:
     *
     * | Route Pattern              | Access Level           | Purpose                    |
     * |----------------------------|------------------------|----------------------------|
     * | /api/v1/auth/**            | Public (permitAll)     | Authentication operations  |
     * | /api/v1/products/**        | Public (permitAll)     | Product catalog (read-only)|
     * | /api/v1/categories/**      | Public (permitAll)     | Category browsing          |
     * | /api/health, /actuator/**  | Public (permitAll)     | Health monitoring          |
     * | /api/v1/admin/**           | ADMIN role required    | Administrative operations  |
     * | /** (all other routes)     | Authenticated required | User-specific operations   |
     *
     * @param http HttpSecurity configuration builder
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Disabled: JWT-based auth (stateless)
                .cors(Customizer.withDefaults()) // Enabled: Cross-origin requests allowed

                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // STATELESS: No HTTP sessions

                .headers(headers -> headers
                        // Prevent clickjacking (deny embedding in iframes)
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)

                        // Prevent MIME sniffing (enforce declared Content-Type)
                        .contentTypeOptions(Customizer.withDefaults())

                        // XSS protection (legacy, but still useful)
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))

                        // HSTS (force HTTPS for 1 year)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000) // 1 year
                                .includeSubDomains(true)
                                .preload(true))

                        // Content Security Policy (restrict resource loading)
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self'; " +
                                                "style-src 'self' 'unsafe-inline'; " + // Allow inline styles (for frameworks)
                                                "img-src 'self' data: https:; " + // Allow images from HTTPS and data URIs
                                                "font-src 'self'; " +
                                                "connect-src 'self'; " +
                                                "frame-ancestors 'none'" // Prevent embedding (same as X-Frame-Options)
                                ))

                        // Referrer policy (control referrer header)
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

                        // Permissions policy (disable unnecessary browser features)
                        .permissionsPolicy(permissions -> permissions
                                .policy("geolocation=(), microphone=(), camera=()"))
                )

                // =========================
                // AUTHORIZATION RULES
                // =========================
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(ApiConstants.PUBLIC_ENDPOINTS).permitAll()

                        // Admin endpoints (ROLE_ADMIN required)
                        .requestMatchers(ApiConstants.ADMIN_BASE + "/**").hasRole("ADMIN")

                        // Default: All other routes require authentication
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
