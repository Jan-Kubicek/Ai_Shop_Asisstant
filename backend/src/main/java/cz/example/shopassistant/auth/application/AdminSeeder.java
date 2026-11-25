package cz.example.shopassistant.auth.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import cz.example.shopassistant.users.UsersApi;

/**
 * Založí prvního admina při spuštění s parametrem {@code --seed-admin}.
 *
 * <p>Údaje se čtou z proměnných {@code SEED_ADMIN_EMAIL} a
 * {@code SEED_ADMIN_PASSWORD}. Seed se provede pouze nad prázdnou kolekcí
 * uživatelů, takže opakované spuštění nic nepřepíše.</p>
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsersApi users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminSeeder(
            UsersApi users,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed-admin.email:}") String email,
            @Value("${app.seed-admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("seed-admin")) {
            return;
        }

        if (users.count() > 0) {
            LOG.info("Seed admina přeskočen: uživatelé už existují.");
            return;
        }

        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Pro --seed-admin nastavte SEED_ADMIN_EMAIL a SEED_ADMIN_PASSWORD.");
        }

        users.create(email, passwordEncoder.encode(password), "ADMIN", 100);
        LOG.info("Admin {} byl vytvořen.", email);
    }
}
