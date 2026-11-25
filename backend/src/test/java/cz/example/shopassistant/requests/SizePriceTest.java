package cz.example.shopassistant.requests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.ai.application.ModelPromptValidator;
import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.requests.application.RequestsService;
import cz.example.shopassistant.requests.application.RequestsService.CreatedRequestView;
import cz.example.shopassistant.requests.domain.Request;
import cz.example.shopassistant.requests.domain.RequestRepository;

class SizePriceTest {

    @Test
    void tabulkaVelikosti68Az152SKrokem6A5Procenty() {
        RequestRepository repository = mock(RequestRepository.class);
        MediaApi mediaApi = mock(MediaApi.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        when(mediaApi.store(anyString(), any(MultipartFile.class))).thenReturn(
                new MediaApi.MediaFile("m1", "u1", "image/png", 1L, "x.png", Instant.now()));
        when(repository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestsService service = new RequestsService(
                repository, mediaApi, new ObjectMapper(), events, mock(ModelPromptValidator.class));

        String options = """
                {
                  "generateWebText": true,
                  "generateSocialPost": true,
                  "generateModelPhoto": false,
                  "sizeFrom": "68",
                  "sizeTo": "152",
                  "basePrice": 490,
                  "priceIncreasePercent": 5
                }
                """;

        CreatedRequestView view = service.create(
                "u1", new MockMultipartFile("photo", "x.png", "image/png", new byte[] {1}),
                "Testovaci cepice", options);

        assertThat(view.sizePrices()).hasSize(15);
        assertThat(view.sizePrices().get(0).size()).isEqualTo(68);
        assertThat(view.sizePrices().get(0).price()).isEqualByComparingTo("490");
        assertThat(view.sizePrices().get(1).size()).isEqualTo(74);
        assertThat(view.sizePrices().get(1).price()).isEqualByComparingTo("515");
        assertThat(view.sizePrices().get(14).size()).isEqualTo(152);
        assertThat(view.sizePrices().get(14).price()).isEqualByComparingTo("971");
    }
}
