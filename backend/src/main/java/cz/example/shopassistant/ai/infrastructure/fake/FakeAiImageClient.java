package cz.example.shopassistant.ai.infrastructure.fake;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.example.shopassistant.ai.application.AiException;
import cz.example.shopassistant.ai.application.AiImageClient;
import cz.example.shopassistant.ai.application.AiImageResult;

/**
 * Falešný image skill – vrátí deterministický obrázek 1000 × 1000 bez API volání.
 */
@Component
@ConditionalOnProperty(name = "app.ai.image-provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiImageClient implements AiImageClient {

    @Override
    public AiImageResult generate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new AiException("Chybí popis modelu pro vygenerování fotky.");
        }

        BufferedImage image = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(238, 242, 238));
        graphics.fillRect(0, 0, 1000, 1000);
        graphics.setColor(new Color(31, 37, 33));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        graphics.drawString("MODEL PHOTO (FAKE)", 150, 470);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        graphics.drawString("1000 x 1000 px", 150, 520);
        graphics.dispose();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return new AiImageResult(output.toByteArray(), "image/png", "model-photo-fake");
        } catch (Exception exception) {
            throw new AiException("Fake obrázek se nepodařilo vytvořit.", exception);
        }
    }
}
