// NEW FILE: src/main/java/com/yash/fineshyttt/service/media/PresignedUpload.java
package com.yash.fineshyttt.service.media;

import java.time.Instant;

public record PresignedUpload(
        String uploadUrl,
        String publicUrl,
        Instant expiresAt
) {}
