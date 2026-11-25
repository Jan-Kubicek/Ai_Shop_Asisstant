package cz.example.shopassistant.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.ai.application.OutputValidator;
import cz.example.shopassistant.export.application.ExportService;
import cz.example.shopassistant.export.domain.RequestResultRepository;
import cz.example.shopassistant.export.ExportApi.ResultData;

import static org.mockito.Mockito.mock;

class ExportServiceTest {

    private static final String WEB = """
            {
              "title": "Ručně pletená čepice, šedá",
              "shortDescription": "Teplá čepice z čisté vlny.",
              "longDescription": "Delší popis produktu, který má alespoň padesát znaků a popisuje čepici podrobně.",
              "keywords": ["čepice", "vlna", "zima"]
            }
            """;

    private static final String SOCIAL = """
            {
              "instagram": {"caption": "Teplá čepice na zimu.", "hashtags": ["#čepice", "#vlna", "#zima"]},
              "facebook": {"text": "Ručně pletená čepice z vlny pro chladné dny."}
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ExportService service = new ExportService(mock(RequestResultRepository.class), mapper);

    @Test
    void wooJsonOdpovidaSchematu() {
        String wooJson = service.buildWooJson("req-1", WEB, SOCIAL, "media-1", java.util.List.of());

        new OutputValidator(mapper).validate(wooJson, "schemas/woo-product.schema.json");

        assertThat(wooJson).contains("\"schemaVersion\":\"1.0\"");
        assertThat(wooJson).contains("/api/v1/media/media-1");
    }

    @Test
    void zipObsahujeTextyAExport() {
        ResultData data = new ResultData("id", "req-1", WEB, SOCIAL,
                service.buildWooJson("req-1", WEB, SOCIAL, "media-1", java.util.List.of()),
                null, null, null, null);

        byte[] zip = service.createZip(data, "image".getBytes(StandardCharsets.UTF_8));

        java.util.List<String> names = new java.util.ArrayList<>();
        try (ZipInputStream input = new ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }

        assertThat(names).contains("product-text.json", "social-text.json", "woo-product.json",
                "product-image.bin");
    }
}
