package cz.example.shopassistant.pipeline.application;

import org.springframework.stereotype.Service;

import cz.example.shopassistant.pipeline.PipelineApi;
import cz.example.shopassistant.pipeline.domain.AiCall;
import cz.example.shopassistant.pipeline.domain.AiCallRepository;

@Service
public class PipelineStatsService implements PipelineApi {

    private final AiCallRepository repository;

    public PipelineStatsService(AiCallRepository repository) {
        this.repository = repository;
    }

    @Override
    public long callsCount() {
        return repository.count();
    }

    @Override
    public long totalTokens() {
        return totalPromptTokens() + totalCompletionTokens();
    }

    @Override
    public long totalPromptTokens() {
        return repository.findAll().stream().mapToLong(AiCall::getPromptTokens).sum();
    }

    @Override
    public long totalCompletionTokens() {
        return repository.findAll().stream().mapToLong(AiCall::getCompletionTokens).sum();
    }

    @Override
    public java.util.List<DailyTokens> dailyTokens(int days) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.Map<java.time.LocalDate, Long> grouped = new java.util.HashMap<>();
        for (var call : repository.findAll()) {
            if (call.getTimestamp() == null) {
                continue;
            }
            java.time.LocalDate day = call.getTimestamp()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            grouped.merge(day,
                    (long) call.getPromptTokens() + call.getCompletionTokens(),
                    Long::sum);
        }

        java.util.List<DailyTokens> result = new java.util.ArrayList<>();
        for (int i = Math.max(days, 1) - 1; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            result.add(new DailyTokens(day, grouped.getOrDefault(day, 0L)));
        }
        return result;
    }
}
