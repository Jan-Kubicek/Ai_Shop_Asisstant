package cz.example.shopassistant.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import cz.example.shopassistant.audit.AuditApi.AuditEventView;
import cz.example.shopassistant.audit.application.AuditService;
import cz.example.shopassistant.audit.domain.AuditEvent;
import cz.example.shopassistant.audit.domain.AuditEventRepository;

class AuditServiceTest {

    @Test
    void filtrSOperatorniZnakyNevyhodiChybuAJeEscapovany() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.count(any(Query.class), eq(AuditEvent.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(AuditEvent.class))).thenReturn(List.of());

        AuditService service = new AuditService(repository, mongoTemplate);

        Page<AuditEventView> page = service.list(0, 20, "[LOGIN", "a+b", true);

        assertThat(page.getContent()).isEmpty();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        org.mockito.Mockito.verify(mongoTemplate).find(captor.capture(), eq(AuditEvent.class));
        Object action = captor.getValue().getQueryObject().get("action");
        assertThat(action).isInstanceOf(Pattern.class);
        assertThat(((Pattern) action).pattern()).isEqualTo(Pattern.quote("[LOGIN"));
    }
}
