package com.yash.fineshyttt.security;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Component
public class TokenHasher {

    public String hash(String token) {
        return DigestUtils.md5DigestAsHex(
                token.getBytes(StandardCharsets.UTF_8)
        );
    }
}
