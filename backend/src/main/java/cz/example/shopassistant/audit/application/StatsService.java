package cz.example.shopassistant.audit.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cz.example.shopassistant.pipeline.PipelineApi;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.users.UsersApi;

/**
 * Souhrnné statistiky pro administraci včetně denních řad a odhadu nákladů.
 */
@Service
public class StatsService {

    private final RequestsApi requestsApi;
    private final UsersApi usersApi;
    private final PipelineApi pipelineApi;
    private final double inputCostPerMillion;
    private final double outputCostPerMillion;

    public StatsService(
            RequestsApi requestsApi,
            UsersApi usersApi,
            PipelineApi pipelineApi,
            @Value("${app.ai.cost.input-usd-per-million:0.30}") double inputCostPerMillion,
            @Value("${app.ai.cost.output-usd-per-million:2.50}") double outputCostPerMillion) {
        this.requestsApi = requestsApi;
        this.usersApi = usersApi;
        this.pipelineApi = pipelineApi;
        this.inputCostPerMillion = inputCostPerMillion;
        this.outputCostPerMillion = outputCostPerMillion;
    }

    public Stats get() {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        double cost = (pipelineApi.totalPromptTokens() / 1_000_000.0) * inputCostPerMillion
                + (pipelineApi.totalCompletionTokens() / 1_000_000.0) * outputCostPerMillion;

        return new Stats(
                requestsApi.count(),
                requestsApi.countCreatedSince(startOfDay),
                requestsApi.countFailed(),
                pipelineApi.totalTokens(),
                usersApi.countEnabled(),
                Math.round(cost * 10000.0) / 10000.0,
                requestsApi.dailyStats(7),
                pipelineApi.dailyTokens(7));
    }

    public record Stats(
            long requestsTotal,
            long requestsToday,
            long failedTotal,
            long aiTokensTotal,
            long activeUsers,
            double estimatedCostUsd,
            java.util.List<RequestsApi.DailyStat> requestsDaily,
            java.util.List<PipelineApi.DailyTokens> tokensDaily) {
    }
}
