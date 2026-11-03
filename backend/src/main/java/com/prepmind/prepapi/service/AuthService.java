package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.AuthResponse;
import com.prepmind.prepapi.dto.LoginRequest;
import com.prepmind.prepapi.dto.RegisterRequest;
import com.prepmind.prepapi.dto.UserResponse;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.repository.UserRepository;
import com.prepmind.prepapi.security.JwtService;
import com.prepmind.prepapi.security.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    private final Set<String> allowedEmailDomains;
    private final Set<String> adminUsernames;
    /** Checked when the username doesn't exist, so response time doesn't reveal which usernames are real. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginAttemptService loginAttempts,
                       @Value("${app.auth.allowed-email-domains:}") String[] allowedEmailDomains,
                       @Value("${app.admin.usernames:}") String[] adminUsernames) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.allowedEmailDomains = normalize(allowedEmailDomains, true);
        this.adminUsernames = normalize(adminUsernames, false);
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (!allowedEmailDomains.isEmpty()) {
            String domain = email.substring(email.lastIndexOf('@') + 1);
            if (!allowedEmailDomains.contains(domain)) {
                throw ApiException.badRequest("EMAIL_DOMAIN_NOT_ALLOWED",
                        "Registration is limited to these email domains: " + String.join(", ", allowedEmailDomains));
            }
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflict("USERNAME_TAKEN", "That username is already taken.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with that email already exists.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(new HashSet<>(Set.of("ROLE_USER")));
        userRepository.save(user);
        log.info("Registered new user '{}'", username);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {
        String username = request.username().trim();
        String attemptKey = LoginAttemptService.key(username, clientIp);
        loginAttempts.checkAllowed(attemptKey);

        Optional<User> userOpt = userRepository.findByUsername(username);
        boolean passwordOk;
        if (userOpt.isPresent()) {
            passwordOk = passwordEncoder.matches(request.password(), userOpt.get().getPassword());
        } else {
            passwordEncoder.matches(request.password(), dummyHash);
            passwordOk = false;
        }

        if (!passwordOk) {
            loginAttempts.recordFailure(attemptKey);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid username or password.");
        }
        loginAttempts.recordSuccess(attemptKey);

        User user = userOpt.get();
        String sessionId = UUID.randomUUID().toString();
        user.setCurrentSessionId(sessionId); // invalidates tokens issued to any other device
        JwtService.IssuedToken token = jwtService.issue(user.getUsername(), sessionId);
        return new AuthResponse(token.token(), user.getUsername(), token.expiresAt());
    }

    /** Server-side logout: the current token stops working immediately. */
    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(u -> u.setCurrentSessionId(null));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found."));
        List<String> roles = adminUsernames.contains(user.getUsername())
                ? List.of("ROLE_USER", "ROLE_ADMIN") : List.of("ROLE_USER");
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), roles);
    }

    private static Set<String> normalize(String[] values, boolean lowercase) {
        if (values == null) {
            return Set.of();
        }
        return Arrays.stream(values)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> lowercase ? s.toLowerCase(Locale.ROOT) : s)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
