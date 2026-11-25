package cz.example.shopassistant.pipeline;

/**
 * Veřejné rozhraní modulu Pipeline pro statistiky a reporting.
 */
public interface PipelineApi {

    long callsCount();

    long totalTokens();

    long totalPromptTokens();

    long totalCompletionTokens();

    record DailyTokens(java.time.LocalDate date, long tokens) {
    }

    java.util.List<DailyTokens> dailyTokens(int days);
}
