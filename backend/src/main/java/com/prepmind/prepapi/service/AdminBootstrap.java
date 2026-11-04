package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Role;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeds the first admins: every username in app.admin.usernames (ADMIN_USERNAMES) is made ADMIN at
 * startup and whenever it logs in. It only grants, never revokes. Everything else about roles is
 * managed from the admin panel and stored in the database.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final Set<String> adminUsernames;

    public AdminBootstrap(UserRepository userRepository, @Value("${app.admin.usernames:}") String[] adminUsernames) {
        this.userRepository = userRepository;
        this.adminUsernames = adminUsernames == null ? Set.of() : Arrays.stream(adminUsernames)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String name : adminUsernames) {
            userRepository.findByUsernameIgnoreCase(name).ifPresent(this::apply);
        }
    }

    /** Call inside a transaction; the change is flushed with it. */
    public void apply(User user) {
        if (user.effectiveRole() != Role.ADMIN
                && adminUsernames.contains(user.getUsername().toLowerCase(Locale.ROOT))) {
            user.setRole(Role.ADMIN);
            log.info("Granted ADMIN to '{}' (listed in ADMIN_USERNAMES)", user.getUsername());
        }
    }
}
