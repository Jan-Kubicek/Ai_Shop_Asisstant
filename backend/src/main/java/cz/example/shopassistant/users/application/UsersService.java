package cz.example.shopassistant.users.application;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cz.example.shopassistant.users.UsersApi;
import cz.example.shopassistant.users.domain.User;
import cz.example.shopassistant.users.domain.UserRepository;
import cz.example.shopassistant.users.domain.UserRole;

@Service
public class UsersService implements UsersApi {

    private final UserRepository repository;

    public UsersService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return repository.findByEmail(normalize(email)).map(UsersService::toAccount);
    }

    @Override
    public Optional<UserAccount> findById(String id) {
        return repository.findById(id).map(UsersService::toAccount);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countEnabled() {
        return repository.countByEnabledTrue();
    }

    @Override
    public UserAccount create(String email, String passwordHash, String role, int dailyLimit) {
        User user = new User();
        user.setEmail(normalize(email));
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.valueOf(role));
        user.setDailyLimit(dailyLimit);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        return toAccount(repository.save(user));
    }

    @Override
    public void recordLogin(String id) {
        repository.findById(id).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            repository.save(user);
        });
    }

    @Override
    public Page<UserView> list(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return repository.findAll(pageable).map(UsersService::toView);
    }

    @Override
    public UserView createUser(String email, String passwordHash, String role, int dailyLimit) {
        UserAccount account = create(email, passwordHash, role, dailyLimit);
        return repository.findById(account.id())
                .map(UsersService::toView)
                .orElseThrow();
    }

    @Override
    public UserView updateUser(String id, String role, Boolean enabled, Integer dailyLimit) {
        User user = repository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Uživatel neexistuje."));
        if (role != null && !role.isBlank()) {
            user.setRole(UserRole.valueOf(role));
        }
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        if (dailyLimit != null) {
            user.setDailyLimit(Math.max(dailyLimit, 1));
        }
        return toView(repository.save(user));
    }

    private static UserView toView(User user) {
        return new UserView(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                user.getDailyLimit(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserAccount toAccount(User user) {
        return new UserAccount(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.isEnabled(),
                user.getDailyLimit(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }
}
