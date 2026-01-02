package com.yash.fineshyttt.service.media;

import com.yash.fineshyttt.config.MediaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
@Profile({"dev", "test"})  // ✅ Active for dev and test profiles
@RequiredArgsConstructor
@Slf4j
public class LocalMediaStorageService implements MediaStorageService {  // ✅ IMPLEMENTS, not extends

    private final MediaProperties properties;

    @Override
    public String store(MultipartFile file, String folder) throws IOException {
        // Create upload directory if not exists
        Path uploadPath = Paths.get(properties.getUploadDir(), folder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = UUID.randomUUID().toString() + extension;

        // Copy file to target location
        Path targetLocation = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Return public URL
        return properties.getBaseUrl() + "/uploads/" + folder + "/" + filename;
    }

    @Override
    public void delete(String fileUrl) {
        try {
            // Extract path from URL
            String path = fileUrl.replace(properties.getBaseUrl(), "");
            Path filePath = Paths.get(properties.getUploadDir(), path.replace("/uploads/", ""));

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
        }
    }

    @Override
    public PresignedUpload generatePresignedUrl(String folder, String filename) {
        // For local storage, just return direct URL
        String url = properties.getBaseUrl() + "/uploads/" + folder + "/" + filename;
        return new PresignedUpload(
                UUID.randomUUID().toString(),
                url,
                Instant.now().plusSeconds(3600)
        );
    }
}
