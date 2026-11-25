package cz.example.shopassistant.media;

import java.time.Instant;

/**
 * Veřejné rozhraní modulu Media.
 */
public interface MediaApi {

    record MediaFile(String id, String ownerId, String contentType, long sizeBytes, String filename, Instant createdAt) {
    }

    MediaFile store(String ownerId, org.springframework.web.multipart.MultipartFile file);

    MediaFile getOwned(String mediaId, String ownerId);

    byte[] readContent(String mediaId, String ownerId);

    String contentType(String mediaId, String ownerId);

    void delete(String mediaId, String ownerId);
}
