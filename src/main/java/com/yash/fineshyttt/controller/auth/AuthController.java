package com.yash.fineshyttt.controller.auth;

import com.yash.fineshyttt.dto.auth.AuthResponse;
import com.yash.fineshyttt.dto.auth.LoginRequest;
import com.yash.fineshyttt.dto.auth.RefreshRequest;
import com.yash.fineshyttt.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                authService.login(
                        request.email(),
                        request.password(),
                        request.deviceFingerprint()
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(
                authService.refresh(request.refreshToken())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal(expression = "user") Object user) {
        authService.logout((com.yash.fineshyttt.domain.User) user);
        return ResponseEntity.noContent().build();
    }
}
