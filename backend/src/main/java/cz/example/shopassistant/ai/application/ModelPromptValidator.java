package cz.example.shopassistant.ai.application;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Bezpečnostní kontrola popisu modelu.
 *
 * <p>Povoluje běžné marketingové popisy včetně zmínky o dítěti v oblečení.
 * Odmítá explicitní nahotu a sexualizaci – zejména u dětského zboží.</p>
 */
@Component
public class ModelPromptValidator {

    private static final int MAX_LENGTH = 2000;
    private static final Pattern BANNED = Pattern.compile(
            "(?i)(nah(y|á|ý|e|ou)|nude|naked|svlékat|svlék|svlékn|bez oblečení|"
                    + "polo ?nah|erot|sexu|porno|genit|pohlavn|orgie|vzruš|odhalen)");

    public void validate(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (text.length() > MAX_LENGTH) {
            throw new AiException("Popis modelu je příliš dlouhý (max. " + MAX_LENGTH + " znaků).");
        }
        if (BANNED.matcher(text).find()) {
            throw new AiException(
                    "Popis modelu obsahuje nepovolený obsah (nahota nebo sexualizace). "
                            + "Uveďte prosím jen oblečení a neutrální scénu.");
        }
    }
}
