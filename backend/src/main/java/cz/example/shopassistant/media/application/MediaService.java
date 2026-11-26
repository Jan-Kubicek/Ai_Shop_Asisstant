package cz.example.shopassistant.media.application;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
        return storeBytes(ownerId, raw);
    }

    @Override
    public MediaApi.MediaFile storeBytes(String ownerId, byte[] content, String filename,
            String contentType) {
        if (content == null || content.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Obrázek je prázdný.");
        }
        if (content.length > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Obrázek je příliš velký (max. " + (maxBytes / (1024 * 1024)) + " MB).");
        }
        return storeBytes(ownerId, content);
    }

    private MediaApi.MediaFile storeBytes(String ownerId, byte[] raw) {
        String format = detectFormat(raw);
        if (format == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Podporované formáty jsou JPG, PNG, BMP a WebP.");
        }

        byte[] processed = normalize(raw, format);
        String sha256 = sha256(processed);

        cz.example.shopassistant.media.domain.MediaFile existing = repository.findByOwnerIdAndSha256(ownerId, sha256).orElse(null);
        if (existing != null) {
            return toApi(existing);
        }

        String filename = UUID.randomUUID() + "." + extension(format);
        Path target = storageRoot.resolve(filename);
        try {
            Files.createDirectories(storageRoot);
            Files.write(target, processed);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Fotku se nepodařilo uložit.");
        }

        cz.example.shopassistant.media.domain.MediaFile media = new cz.example.shopassistant.media.domain.MediaFile();
        media.setOwnerId(ownerId);
        media.setFilename(filename);
        media.setContentType(contentType(format));
        media.setSizeBytes(processed.length);
        media.setSha256(sha256);
        media.setStoragePath(target.toString());
        media.setCreatedAt(Instant.now());

        return toApi(repository.save(media));
    }

    @Override
    public MediaApi.MediaFile getOwned(String mediaId, String ownerId) {
        cz.example.shopassistant.media.domain.MediaFile media = repository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Soubor neexistuje."));
        if (!media.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Soubor neexistuje.");
        }
        return toApi(media);
    }

    @Override
    public byte[] readContent(String mediaId, String ownerId) {
        MediaApi.MediaFile api = getOwned(mediaId, ownerId);
        cz.example.shopassistant.media.domain.MediaFile entity = repository.findById(api.id()).orElseThrow();
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
        if (bytes.length >= 2 && bytes[0] == 'B' && bytes[1] == 'M') {
            return "bmp";
        }
        return null;
    }

    /**
     * Převede obrázek na čtverec 1000 × 1000 px. Delší strana se přizpůsobí
     * 1000 px a zbývající plocha se doplní bílými pásy (u svislého obrázku
     * vpravo a vlevo, u vodorovného nahoře a dole). Čtverec se jen převede.
     * Rekódování zároveň odstraní EXIF metadata.
     */
    private byte[] normalize(byte[] raw, String format) {
        if ("webp".equals(format)) {
            // ImageIO v JDK neumí WebP – ponecháme originál beze změny.
            return raw;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(raw);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                return raw;
            }

            int width = image.getWidth();
            int height = image.getHeight();
            double scale = 1000.0 / Math.max(width, height);
            int scaledWidth = Math.max(1, (int) Math.round(width * scale));
            int scaledHeight = Math.max(1, (int) Math.round(height * scale));

            BufferedImage canvas = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = canvas.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 1000, 1000);
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(image, (1000 - scaledWidth) / 2, (1000 - scaledHeight) / 2,
                    scaledWidth, scaledHeight, null);
            graphics.dispose();

            String outFormat = "jpg".equals(format) ? "jpg" : "png";
            boolean written = ImageIO.write(canvas, outFormat, output);
            if (!written || output.size() == 0) {
                return raw;
            }
            return output.toByteArray();
        } catch (IOException exception) {
            return raw;
        }
    }

    @Override
    public void delete(String mediaId, String ownerId) {
        getOwned(mediaId, ownerId);
        cz.example.shopassistant.media.domain.MediaFile entity = repository.findById(mediaId).orElseThrow();
        try {
            Path file = Paths.get(entity.getStoragePath());
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            // Soubor možná už neexistuje – záznam přesto odstraníme.
        }
        repository.delete(entity);
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 není dostupné.", exception);
        }
    }

    private static String extension(String format) {
        return switch (format) {
            case "jpg" -> "jpg";
            case "png" -> "png";
            case "webp" -> "webp";
            case "bmp" -> "png";
            default -> "bin";
        };
    }

    private static String contentType(String format) {
        return switch (format) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "bmp" -> "image/png";
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
