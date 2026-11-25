package cz.example.shopassistant.users.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
        String role = request.role() != null && !request.role().isBlank()
                ? request.role().toUpperCase() : "USER";
        int dailyLimit = request.dailyLimit() != null ? request.dailyLimit() : 20;

        return usersApi.createUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                role,
                dailyLimit);
    }

    @PatchMapping("/{id}")
    public UserView update(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        return usersApi.updateUser(id, request.role(), request.enabled(), request.dailyLimit());
    }

    public record CreateUserRequest(String email, String password, String role, Integer dailyLimit) {
    }

    public record UpdateUserRequest(String role, Boolean enabled, Integer dailyLimit) {
    }

    public record PageResponse<T>(List<T> items, long total, int page, int size) {
    }
}
