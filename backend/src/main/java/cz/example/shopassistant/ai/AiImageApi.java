package cz.example.shopassistant.ai;

/**
 * Veřejné rozhraní image skillu (modelová fotka) a its safety kontroly.
 */
public interface AiImageApi {

    record GeneratedImage(byte[] image, String contentType, String description) {
    }

    void validateModelDescription(String text);

    GeneratedImage generate(String prompt);

    /**
     * Sestaví prompt pro fotku modelu tak, aby obsahoval i popis produktu.
     */
    default String modelPrompt(String productDescription, String modelDescription) {
        return "Photorealistic e-commerce product photography. "
                + "The person is wearing the exact product described here: "
                + (productDescription == null ? "" : productDescription)
                + ". Model and styling: "
                + (modelDescription == null ? "" : modelDescription)
                + ". The garment must be clearly visible and correctly worn, "
                + "realistic fabric texture, natural skin, neutral light-grey studio background, "
                + "soft daylight, sharp focus, 85mm lens, high detail. "
                + "No illustration, no cartoon, no text, no logos, no deformed hands, "
                + "no extra limbs, no nudity, no sexualization. Any child must be fully clothed.";
    }
}
