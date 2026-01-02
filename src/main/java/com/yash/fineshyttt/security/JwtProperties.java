package com.yash.fineshyttt.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.jwt")
@Getter @Setter
public class JwtProperties {
    private String secret;
    private long accessTokenTtlSeconds;
    private long gracePeriodSeconds = 30;
}
