package com.yash.fineshyttt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "media")
@Data
public class MediaProperties {

    private String uploadDir = "uploads";
    private String baseUrl = "http://localhost:8080";
    private long maxFileSize = 5242880; // 5MB
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
}
