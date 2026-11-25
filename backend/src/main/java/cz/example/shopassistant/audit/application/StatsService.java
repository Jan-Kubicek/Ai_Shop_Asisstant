package cz.example.shopassistant.audit.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import cz.example.shopassistant.pipeline.PipelineApi;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.users.UsersApi;

/**
 * Souhrnné statistiky pro administraci.
 */
@Service
public class StatsService {

    private final RequestsApi requestsApi;
    private final UsersApi usersApi;
    private final PipelineApi pipelineApi;

    public StatsService(RequestsApi requestsApi, UsersApi usersApi, PipelineApi pipelineApi) {
        this.requestsApi = requestsApi;
        this.usersApi = usersApi;
        this.pipelineApi = pipelineApi;
    }

    public Stats get() {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        return new Stats(
                requestsApi.count(),
                requestsApi.countCreatedSince(startOfDay),
                requestsApi.countFailed(),
                pipelineApi.totalTokens(),
                usersApi.countEnabled());
    }

    public record Stats(
            long requestsTotal,
            long requestsToday,
            long failedTotal,
            long aiTokensTotal,
            long activeUsers) {
    }
}
