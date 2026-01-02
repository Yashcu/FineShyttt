package com.yash.fineshyttt.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class SecurityPropertiesValidator {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @PostConstruct
    void validate() {
        if (jwtSecret.contains("dev-only") || jwtSecret.contains("please-dont-use")) {
            throw new IllegalStateException("Production JWT secret is not configured!");
        }

        if (dbPassword.equals("postgres")) {
            throw new IllegalStateException("Production database password is insecure!");
        }
    }
}
