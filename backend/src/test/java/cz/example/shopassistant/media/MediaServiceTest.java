package cz.example.shopassistant.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import cz.example.shopassistant.media.application.MediaService;
import cz.example.shopassistant.media.domain.MediaRepository;

class MediaServiceTest {

    @Test
    void originalIdNevyhodiChybuKdyzMediaNeexistuje() {
        MediaRepository repository = mock(MediaRepository.class);
        when(repository.findById("smazane-id")).thenReturn(Optional.empty());
        MediaService service = new MediaService(repository, "./data/uploads", 8);

        assertThat(service.originalId("smazane-id", "user-1")).isNull();
    }

    @Test
    void originalIdVratiNullProCiziMedia() {
        MediaRepository repository = mock(MediaRepository.class);
        cz.example.shopassistant.media.domain.MediaFile media =
                new cz.example.shopassistant.media.domain.MediaFile();
        media.setId("media-1");
        media.setOwnerId("owner-1");
        when(repository.findById("media-1")).thenReturn(Optional.of(media));
        MediaService service = new MediaService(repository, "./data/uploads", 8);

        assertThat(service.originalId("media-1", "jiny-owner")).isNull();
    }
}
