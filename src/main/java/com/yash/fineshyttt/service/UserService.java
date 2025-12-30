package com.yash.fineshyttt.service;

import com.yash.fineshyttt.domain.User;
import com.yash.fineshyttt.exception.AuthenticationException;
import com.yash.fineshyttt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));
    }
}
