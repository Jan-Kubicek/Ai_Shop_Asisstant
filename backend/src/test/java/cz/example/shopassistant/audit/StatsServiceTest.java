package cz.example.shopassistant.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import cz.example.shopassistant.audit.application.StatsService;
import cz.example.shopassistant.pipeline.PipelineApi;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.users.UsersApi;

class StatsServiceTest {

    @Test
    void statsSkladaHodnotyZModulu() {
        RequestsApi requests = mock(RequestsApi.class);
        UsersApi users = mock(UsersApi.class);
        PipelineApi pipeline = mock(PipelineApi.class);

        when(requests.count()).thenReturn(12L);
        when(requests.countCreatedSince(ArgumentMatchers.any(Instant.class))).thenReturn(3L);
        when(requests.countFailed()).thenReturn(1L);
        when(users.countEnabled()).thenReturn(4L);
        when(pipeline.totalTokens()).thenReturn(1500L);

        StatsService.Stats stats = new StatsService(requests, users, pipeline).get();

        assertThat(stats.requestsTotal()).isEqualTo(12);
        assertThat(stats.requestsToday()).isEqualTo(3);
        assertThat(stats.failedTotal()).isEqualTo(1);
        assertThat(stats.aiTokensTotal()).isEqualTo(1500);
        assertThat(stats.activeUsers()).isEqualTo(4);
    }
}
