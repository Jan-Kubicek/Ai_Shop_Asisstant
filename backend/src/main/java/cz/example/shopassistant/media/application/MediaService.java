package cz.example.shopassistant.media.application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.media.domain.MediaFile;
import cz.example.shopassistant.media.domain.MediaRepository;

@Service
public class MediaService implements MediaApi {

    private final MediaRepository repository;
    private final Path storageRoot;
    private final long maxBytes;

    public MediaService(
            MediaRepository repository,
            @Value("${app.storage.local-path:./data/uploads}") String storagePath,
            @Value("${app.upload.max-mb:8}") int maxMb) {
        this.repository = repository;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        this.maxBytes = (long) maxMb * 1024L * 1024L;
    }

    @Override
    public MediaApi.MediaFile store(String ownerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vyberte fotku produktu.");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Fotka je příliš velká (max. " + (maxBytes / (1024 * 1024)) + " MB).");
        }

        byte[] raw;
        try {
            raw = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fotku se nepodařilo načíst.");
        }

        String format = detectFormat(raw);
        if (format == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Podporované formáty jsou JPG, PNG a WebP.");
        }

        byte[] stripped = stripMetadata(raw, format);
        String sha256 = sha256(stripped);

        MediaFile existing = repository.findByOwnerIdAndSha256(ownerId, sha256).orElse(null);
        if (existing != null) {
            return toApi(existing);
        }

        String filename = UUID.randomUUID() + "." + extension(format);
        Path target = storageRoot.resolve(filename);
        try {
            Files.createDirectories(storageRoot);
            Files.write(target, stripped);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Fotku se nepodařilo uložit.");
        }

        MediaFile media = new MediaFile();
        media.setOwnerId(ownerId);
        media.setFilename(filename);
        media.setContentType(contentType(format));
        media.setSizeBytes(stripped.length);
        media.setSha256(sha256);
        media.setStoragePath(target.toString());
        media.setCreatedAt(Instant.now());

        return toApi(repository.save(media));
    }

    @Override
    public MediaApi.MediaFile getOwned(String mediaId, String ownerId) {
        MediaFile media = repository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Soubor neexistuje."));
        if (!media.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Soubor neexistuje.");
        }
        return toApi(media);
    }

    @Override
    public byte[] readContent(String mediaId, String ownerId) {
        MediaFile api = getOwned(mediaId, ownerId);
        MediaFile entity = repository.findById(api.id()).orElseThrow();
        try {
            return Files.readAllBytes(Paths.get(entity.getStoragePath()));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Soubor se nepodařilo načíst.");
        }
    }

    @Override
    public String contentType(String mediaId, String ownerId) {
        return getOwned(mediaId, ownerId).contentType();
    }

    private static String detectFormat(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E
                && bytes[3] == 0x47 && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return "png";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private byte[] stripMetadata(byte[] raw, String format) {
        if ("webp".equals(format)) {
            // WebP EXIF odstraníme v další fázi; zatím ukládáme originál.
            return raw;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(raw);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                return raw;
            }
            String outFormat = "jpg".equals(format) ? "jpg" : "png";
            boolean written = ImageIO.write(image, outFormat, output);
            if (!written || output.size() == 0) {
                return raw;
            }
            return output.toByteArray();
        } catch (IOException exception) {
            return raw;
        }
    }

    private static String extension(String format) {
        return switch (format) {
            case "jpg" -> "jpg";
            case "png" -> "png";
            case "webp" -> "webp";
            default -> "bin";
        };
    }

    private static String contentType(String format) {
        return switch (format) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private static MediaApi.MediaFile toApi(cz.example.shopassistant.media.domain.MediaFile entity) {
        return new MediaApi.MediaFile(
                entity.getId(),
                entity.getOwnerId(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getFilename(),
                entity.getCreatedAt());
    }
}
