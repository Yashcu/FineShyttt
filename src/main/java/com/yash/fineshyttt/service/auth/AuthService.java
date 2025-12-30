package com.yash.fineshyttt.service.auth;

import com.yash.fineshyttt.domain.User;
import com.yash.fineshyttt.dto.auth.AuthResponse;
import com.yash.fineshyttt.exception.AuthenticationException;
import com.yash.fineshyttt.security.JwtService;
import com.yash.fineshyttt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(String email, String password, String deviceFingerprint) {
        User user = userService.findByEmail(email);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshTokenResult refresh =
                refreshTokenService.create(user, deviceFingerprint);

        return new AuthResponse(
                accessToken,
                refresh.rawValue()
        );
    }

    public AuthResponse refresh(String rawRefreshToken) {
        User user = refreshTokenService.validateAndRotate(rawRefreshToken);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshTokenResult refresh =
                refreshTokenService.create(user, "rotated");

        return new AuthResponse(
                accessToken,
                refresh.rawValue()
        );
    }

    public void logout(User user) {
        refreshTokenService.revokeAllForUser(user.getId());
    }
}
