package cz.example.shopassistant.pipeline.application;

import org.springframework.stereotype.Service;

import cz.example.shopassistant.pipeline.PipelineApi;
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
        return repository.findAll().stream()
                .mapToLong(call -> call.getPromptTokens() + (long) call.getCompletionTokens())
                .sum();
    }
}
