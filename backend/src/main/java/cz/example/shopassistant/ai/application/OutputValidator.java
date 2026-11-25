package cz.example.shopassistant.ai.application;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Vyčistí odpověď AI (odstraní markdown bloky) a ověří ji proti JSON schématu.
 */
@Component
public class OutputValidator {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
    private static final Pattern FIRST_OBJECT = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    public OutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode validate(String raw, String schemaResource) {
        if (raw == null || raw.isBlank()) {
            throw new AiException("Odpověď AI je prázdná.");
        }

        String sanitized = sanitize(raw);
        JsonNode data;
        try {
            data = objectMapper.readTree(sanitized);
        } catch (IOException exception) {
            throw new AiException("Odpověď AI není platný JSON: " + exception.getMessage(), exception);
        }

        JsonNode schema;
        try {
            schema = objectMapper.readTree(new ClassPathResource(schemaResource).getInputStream());
        } catch (IOException exception) {
            throw new IllegalStateException("Schéma " + schemaResource + " se nepodařilo načíst.", exception);
        }

        validateAgainstSchema(data, schema, "");
        return data;
    }

    private String sanitize(String raw) {
        Matcher block = JSON_BLOCK.matcher(raw);
        if (block.find()) {
            return block.group(1).trim();
        }
        Matcher object = FIRST_OBJECT.matcher(raw);
        if (object.find()) {
            return object.group().trim();
        }
        return raw.trim();
    }

    private void validateAgainstSchema(JsonNode data, JsonNode schema, String path) {
        String type = schema.path("type").asText("");
        if (!type.isEmpty() && !matchesType(data, type)) {
            throw new AiException("Pole " + (path.isEmpty() ? "/" : path) + " má špatný typ, očekáváno " + type + ".");
        }

        if (schema.has("required") && data.isObject()) {
            for (JsonNode required : schema.get("required")) {
                String field = required.asText();
                if (!data.has(field) || data.get(field).isNull()) {
                    throw new AiException("Chybí povinné pole: " + field + ".");
                }
            }
        }

        if (schema.path("additionalProperties").asBoolean(true) == false && data.isObject() && schema.has("properties")) {
            for (String field : iterable(data.fieldNames())) {
                if (!schema.get("properties").has(field)) {
                    throw new AiException("Neočekávané pole: " + field + ".");
                }
            }
        }

        if (data.isObject() && schema.has("properties")) {
            schema.get("properties").fields().forEachRemaining(entry -> {
                String field = entry.getKey();
                if (data.has(field) && !data.get(field).isNull()) {
                    validateAgainstSchema(data.get(field), entry.getValue(), path + "/" + field);
                }
            });
        }

        if (data.isArray() && schema.has("items")) {
            for (JsonNode item : data) {
                validateAgainstSchema(item, schema.get("items"), path + "[]");
            }
        }

        if (data.isTextual()) {
            int min = schema.path("minLength").asInt(0);
            int max = schema.path("maxLength").asInt(Integer.MAX_VALUE);
            int length = data.asText().length();
            if (length < min) {
                throw new AiException("Pole " + path + " je příliš krátké (min. " + min + ").");
            }
            if (length > max) {
                throw new AiException("Pole " + path + " je příliš dlouhé (max. " + max + ").");
            }
        }

        if (data.isArray()) {
            int min = schema.path("minItems").asInt(0);
            int max = schema.path("maxItems").asInt(Integer.MAX_VALUE);
            if (data.size() < min) {
                throw new AiException("Pole " + path + " má příliš málo položek (min. " + min + ").");
            }
            if (data.size() > max) {
                throw new AiException("Pole " + path + " má příliš mnoho položek (max. " + max + ").");
            }
        }

        if (data.isArray() && schema.has("items") && schema.get("items").has("minLength")) {
            for (JsonNode item : data) {
                if (item.isTextual() && item.asText().length() < schema.get("items").path("minLength").asInt(0)) {
                    throw new AiException("Položka v " + path + " je příliš krátká.");
                }
            }
        }
    }

    private boolean matchesType(JsonNode data, String type) {
        return switch (type) {
            case "object" -> data.isObject();
            case "array" -> data.isArray();
            case "string" -> data.isTextual();
            case "number" -> data.isNumber();
            case "integer" -> data.isInt();
            case "boolean" -> data.isBoolean();
            default -> true;
        };
    }

    private Iterable<String> iterable(java.util.Iterator<String> iterator) {
        java.util.List<String> list = new java.util.ArrayList<>();
        iterator.forEachRemaining(list::add);
        return list;
    }
}
