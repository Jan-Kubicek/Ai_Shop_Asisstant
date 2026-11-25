package cz.example.shopassistant.ai.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Načítá promptové šablony z {@code resources/prompts/} a dosazuje proměnné
 * ve tvaru {@code {{nazev}}}.
 */
@Component
public class PromptLoader {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    public String load(String name) {
        ClassPathResource resource = new ClassPathResource("prompts/" + name);
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Prompt " + name + " se nepodařilo načíst.", exception);
        }
    }

    public String render(String name, Map<String, String> variables) {
        String template = load(name);
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
