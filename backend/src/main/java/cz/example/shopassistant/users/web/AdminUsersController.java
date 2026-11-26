package cz.example.shopassistant.users.web;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.example.shopassistant.users.UsersApi;
import cz.example.shopassistant.users.UsersApi.UserView;
import cz.example.shopassistant.users.domain.UserRole;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UsersApi usersApi;
    private final PasswordEncoder passwordEncoder;

    public AdminUsersController(UsersApi usersApi, PasswordEncoder passwordEncoder) {
        this.usersApi = usersApi;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public PageResponse<UserView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = usersApi.list(page, size);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @PostMapping
    public UserView create(@RequestBody CreateUserRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zadejte e-mail.");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zadejte heslo.");
        }
        if (request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Heslo musí mít alespoň 8 znaků.");
        }
        String role = normalizeRole(request.role());
        int dailyLimit = validateLimit(request.dailyLimit() != null ? request.dailyLimit() : 20);

        return usersApi.createUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                role,
                dailyLimit);
    }

    @PatchMapping("/{id}")
    public UserView update(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        String role = request.role() == null ? null : normalizeRole(request.role());
        Integer dailyLimit = request.dailyLimit() == null ? null : validateLimit(request.dailyLimit());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof cz.example.shopassistant.common.security.AuthenticatedUser principal
                && id.equals(principal.id())
                && ("USER".equals(role) || Boolean.FALSE.equals(request.enabled()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nemůžete si sami odebrat administrátorská práva ani zablokovat svůj účet.");
        }

        return usersApi.updateUser(id, role, request.enabled(), dailyLimit);
    }

    private static String normalizeRole(String value) {
        if (value == null || value.isBlank()) {
            return UserRole.USER.name();
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Neplatná role. Povolené hodnoty: USER, ADMIN.");
        }
    }

    private static int validateLimit(int value) {
        if (value < 1 || value > 10_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Denní limit musí být mezi 1 a 10000.");
        }
        return value;
    }

    public record CreateUserRequest(String email, String password, String role, Integer dailyLimit) {
    }

    public record UpdateUserRequest(String role, Boolean enabled, Integer dailyLimit) {
    }

    public record PageResponse<T>(List<T> items, long total, int page, int size) {
    }
}
