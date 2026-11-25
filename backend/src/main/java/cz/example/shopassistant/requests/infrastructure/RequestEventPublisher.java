package cz.example.shopassistant.requests.infrastructure;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cz.example.shopassistant.requests.RequestStatusChangedEvent;

/**
 * Registry SSE připojení. Každý klient dostane okamžitě aktuální stav
 * a následně každou změnu stavu svého požadavku.
 */
@Component
public class RequestEventPublisher {

    private static final long TIMEOUT_MS = 15 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(RequestStatusChangedEvent initial) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(initial.requestId(), ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(initial.requestId(), emitter));
        emitter.onTimeout(() -> remove(initial.requestId(), emitter));
        emitter.onError(exception -> remove(initial.requestId(), emitter));

        send(emitter, initial);
        return emitter;
    }

    @EventListener
    public void onStatusChanged(RequestStatusChangedEvent event) {
        List<SseEmitter> listeners = emitters.get(event.requestId());
        if (listeners == null) {
            return;
        }
        for (SseEmitter emitter : listeners) {
            send(emitter, event);
        }
    }

    private void send(SseEmitter emitter, RequestStatusChangedEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(event));
        } catch (IOException exception) {
            removeFromAll(emitter);
        }
    }

    private void remove(String requestId, SseEmitter emitter) {
        List<SseEmitter> listeners = emitters.get(requestId);
        if (listeners != null) {
            listeners.remove(emitter);
            if (listeners.isEmpty()) {
                emitters.remove(requestId);
            }
        }
    }

    private void removeFromAll(SseEmitter emitter) {
        emitters.values().forEach(listeners -> listeners.remove(emitter));
    }
}
