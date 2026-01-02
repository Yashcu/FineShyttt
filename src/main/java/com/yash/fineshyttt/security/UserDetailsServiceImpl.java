package com.yash.fineshyttt.security;

import com.yash.fineshyttt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return new UserPrincipal(userService.findByEmail(email));
    }

    public UserDetails loadUserById(Long userId) {
        return new UserPrincipal(userService.findByIdWithRoles(userId));
    }
}
