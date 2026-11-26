package cz.example.shopassistant.requests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.ai.AiImageApi;
import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.requests.application.RequestsService;
import cz.example.shopassistant.requests.domain.Request;
import cz.example.shopassistant.requests.domain.RequestRepository;
import cz.example.shopassistant.users.UsersApi;

class DailyLimitTest {

    private static final String OPTIONS = """
            {"generateWebText":true,"generateSocialPost":true,"generateModelPhoto":false}
            """;

    private RequestsService service(RequestRepository repository, UsersApi usersApi,
            int modelPhotoLimit) {
        MediaApi mediaApi = mock(MediaApi.class);
        when(mediaApi.store(anyString(), any())).thenReturn(new MediaApi.MediaFile(
                "m1", "u1", "image/png", 1L, "x.png", Instant.now()));
        return new RequestsService(
                repository, mediaApi, new ObjectMapper(), mock(ApplicationEventPublisher.class),
                mock(AiImageApi.class), usersApi, modelPhotoLimit);
    }

    @Test
    void denniLimitPoctupozadavkuVrati429() {
        RequestRepository repository = mock(RequestRepository.class);
        UsersApi usersApi = mock(UsersApi.class);
        when(usersApi.findById("u1")).thenReturn(Optional.of(new UsersApi.UserAccount(
                "u1", "a@example.com", "hash", "USER", true, 1, Instant.now(), null)));
        when(repository.findByUserIdAndCreatedAtAfter(anyString(), any(Instant.class)))
                .thenReturn(List.of(new Request()));

        assertThatThrownBy(() -> service(repository, usersApi, 3).create(
                "u1", photo(), "Popis produktu", OPTIONS))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void denniLimitModelovychFotekVrati429() {
        RequestRepository repository = mock(RequestRepository.class);
        UsersApi usersApi = mock(UsersApi.class);
        when(usersApi.findById("u1")).thenReturn(Optional.of(new UsersApi.UserAccount(
                "u1", "a@example.com", "hash", "USER", true, 100, Instant.now(), null)));

        Request withPhoto = new Request();
        Request.RequestOptions options = new Request.RequestOptions();
        options.setGenerateModelPhoto(true);
        withPhoto.setOptions(options);
        when(repository.findByUserIdAndCreatedAtAfter(anyString(), any(Instant.class)))
                .thenReturn(List.of(withPhoto));

        assertThatThrownBy(() -> service(repository, usersApi, 1).create(
                "u1", photo(), "Popis produktu", OPTIONS.replace("false", "true")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile("photo", "x.png", "image/png", new byte[] {1});
    }
}
