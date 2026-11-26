package cz.example.shopassistant.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cz.example.shopassistant.users.application.UsersService;
import cz.example.shopassistant.users.domain.User;
import cz.example.shopassistant.users.domain.UserRepository;

class UsersServiceTest {

    @Test
    void duplicitniEmailVratiConflict() {
        UserRepository repository = mock(UserRepository.class);
        User existing = new User();
        existing.setEmail("admin@example.com");
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        UsersService service = new UsersService(repository);

        assertThatThrownBy(() -> service.create(" ADMIN@example.com ", "hash", "USER", 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void neplatnaRoleVratiBadRequest() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmail(any(String.class))).thenReturn(Optional.empty());
        UsersService service = new UsersService(repository);

        assertThatThrownBy(() -> service.create("novy@example.com", "hash", "SUPERUSER", 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void roleSeNormalizujeANastaviVychoziLimit() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmail("novy@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UsersService service = new UsersService(repository);

        var account = service.create(" Novy@Example.com ", "hash", "admin", 20);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(account.role()).isEqualTo("ADMIN");
        assertThat(captor.getValue().getEmail()).isEqualTo("novy@example.com");
        assertThat(captor.getValue().isEnabled()).isTrue();
    }
}
