package com.yash.fineshyttt.service.media;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface MediaStorageService {

    /**
     * Store a file and return its public URL
     */
    String store(MultipartFile file, String folder) throws IOException;

    /**
     * Delete a file by its URL
     */
    void delete(String fileUrl);

    /**
     * Generate a presigned URL for direct upload
     */
    PresignedUpload generatePresignedUrl(String folder, String filename);
}
