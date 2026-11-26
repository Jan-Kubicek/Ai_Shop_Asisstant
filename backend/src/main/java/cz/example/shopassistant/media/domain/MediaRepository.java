package cz.example.shopassistant.media.domain;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MediaRepository extends MongoRepository<MediaFile, String> {

    Optional<MediaFile> findByOwnerIdAndSha256(String ownerId, String sha256);

    Optional<MediaFile> findByGroupIdAndVariant(String groupId, String variant);
}
