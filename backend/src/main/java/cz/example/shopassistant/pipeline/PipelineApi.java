package cz.example.shopassistant.pipeline;

/**
 * Veřejné rozhraní modulu Pipeline pro statistiky a reporting.
 */
public interface PipelineApi {

    long callsCount();

    long totalTokens();
}
