package com.yash.fineshyttt.service;

import com.yash.fineshyttt.domain.Role;
import com.yash.fineshyttt.domain.User;
import com.yash.fineshyttt.exception.AuthenticationException;
import com.yash.fineshyttt.repository.RoleRepository;
import com.yash.fineshyttt.repository.UserRepository;
// REMOVE: import com.yash.fineshyttt.security.CachedUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ROLE_USER = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
    }

    @Transactional(readOnly = true)
    public User findByIdWithRoles(Long userId) {
        return userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
    }

    @Transactional
    public void registerCustomer(String email, String rawPassword) {
        String normalizedEmail = normalize(email);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Email already registered");
        }

        Role userRole = roleRepository.findByName(ROLE_USER)
                .orElseThrow(() ->
                        new IllegalStateException("Required role ROLE_USER not found"));

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(rawPassword)
        );

        user.getRoles().add(userRole);

        user.setEmailVerified(true);

        userRepository.save(user);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
