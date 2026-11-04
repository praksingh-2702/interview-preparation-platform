package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.AuthResponse;
import com.prepmind.prepapi.dto.LoginRequest;
import com.prepmind.prepapi.dto.RegisterRequest;
import com.prepmind.prepapi.dto.RegisterResponse;
import com.prepmind.prepapi.dto.ResetPasswordRequest;
import com.prepmind.prepapi.dto.VerifyEmailRequest;
import com.prepmind.prepapi.entity.OtpPurpose;
import com.prepmind.prepapi.entity.Role;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.exception.TooManyRequestsException;
import com.prepmind.prepapi.google.GoogleIdentity;
import com.prepmind.prepapi.google.GoogleTokenVerifier;
import com.prepmind.prepapi.repository.UserRepository;
import com.prepmind.prepapi.security.JwtService;
import com.prepmind.prepapi.security.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Account lifecycle: register → verify email with a 6-digit code → log in.
 * Forgot password works the same way (code by email → new password).
 * Sign in with Google skips the code (Google has verified the email) and links to an existing
 * account with the same email.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String GENERIC_RESET_MESSAGE =
            "If an account exists for that email, we've sent a code to it.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    private final OtpService otpService;
    private final Set<String> allowedEmailDomains;
    private final AdminBootstrap adminBootstrap;
    private final GoogleTokenVerifier googleVerifier;
    private final java.security.SecureRandom random = new java.security.SecureRandom();
    /** Checked when the account doesn't exist, so response time doesn't reveal which accounts are real. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginAttemptService loginAttempts,
                       OtpService otpService,
                       @Value("${app.auth.allowed-email-domains:}") String[] allowedEmailDomains,
                       AdminBootstrap adminBootstrap,
                       GoogleTokenVerifier googleVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.otpService = otpService;
        this.allowedEmailDomains = normalize(allowedEmailDomains, true);
        this.adminBootstrap = adminBootstrap;
        this.googleVerifier = googleVerifier;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // ------------------------------------------------------------------ register + verify

    /**
     * Creates an unverified account and emails a code. Signing up again with the email of an account
     * that was never verified replaces that pending sign-up (so a typo'd or abandoned attempt never
     * blocks the address).
     */
    @Transactional(noRollbackFor = ApiException.class)
    public RegisterResponse register(RegisterRequest request) {
        String username = Usernames.requireValid(request.username());
        String email = normalizeEmail(request.email());
        checkDomain(email);

        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent() && byEmail.get().hasVerifiedEmail()) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with that email already exists. Try logging in.");
        }
        Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(username);
        boolean usernameBelongsToSomeoneElse = byUsername.isPresent()
                && (byEmail.isEmpty() || !byUsername.get().getId().equals(byEmail.get().getId()));
        if (usernameBelongsToSomeoneElse) {
            throw ApiException.conflict("USERNAME_TAKEN", "That username is already taken.");
        }

        User user = byEmail.orElseGet(User::new);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);
        user.setCurrentSessionId(null);
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        userRepository.save(user);

        try {
            otpService.issue(user, OtpPurpose.VERIFY_EMAIL);
        } catch (TooManyRequestsException e) {
            // A code was sent moments ago (e.g. double-submit); the existing one is still valid.
        }
        log.info("Registered user '{}' (pending email verification)", username);
        return new RegisterResponse("We've emailed a 6-digit code to " + email + ". Enter it to activate your account.", email);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> ApiException.badRequest("OTP_INVALID", "Incorrect code."));
        if (user.hasVerifiedEmail()) {
            return; // idempotent: verifying twice is fine
        }
        otpService.verify(user, OtpPurpose.VERIFY_EMAIL, request.code());
        user.setEmailVerified(true);
        log.info("Verified email for '{}'", user.getUsername());
    }

    /** Always succeeds silently for unknown/verified emails so it can't be used to probe accounts. */
    @Transactional(noRollbackFor = ApiException.class)
    public void resendVerification(String rawEmail) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(rawEmail))
                .filter(u -> !u.hasVerifiedEmail())
                .ifPresent(u -> otpService.issue(u, OtpPurpose.VERIFY_EMAIL));
    }

    // ------------------------------------------------------------------ login / logout

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest request, String clientIp) {
        String identifier = request.username().trim();
        String attemptKey = LoginAttemptService.key(identifier, clientIp);
        loginAttempts.checkAllowed(attemptKey);

        Optional<User> userOpt = identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(normalizeEmail(identifier))
                : userRepository.findByUsername(identifier).or(() -> userRepository.findByUsernameIgnoreCase(identifier));

        boolean passwordOk;
        if (userOpt.isPresent()) {
            passwordOk = passwordEncoder.matches(request.password(), userOpt.get().getPassword());
        } else {
            passwordEncoder.matches(request.password(), dummyHash);
            passwordOk = false;
        }
        if (!passwordOk) {
            loginAttempts.recordFailure(attemptKey);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid username/email or password.");
        }
        loginAttempts.recordSuccess(attemptKey);

        User user = userOpt.get();
        if (!user.hasVerifiedEmail()) {
            try {
                otpService.issue(user, OtpPurpose.VERIFY_EMAIL);
            } catch (TooManyRequestsException e) {
                // recent code still valid
            }
            throw ApiException.forbidden("EMAIL_NOT_VERIFIED",
                            "Please verify your email first. We've sent a code to " + user.getEmail() + ".")
                    .with("email", user.getEmail());
        }

        return startSession(user);
    }

    // ------------------------------------------------------------------ Google

    /**
     * Signs in with a Google ID token. Matches the Google account first, then the email: an existing
     * account with that email is linked (same account, same progress). A brand-new user gets a
     * placeholder username and needsUsername=true, so the app asks them to pick one.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse googleLogin(String credential) {
        if (!googleVerifier.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_SIGNIN_DISABLED",
                    "Google sign-in isn't set up on this server yet.");
        }
        GoogleIdentity google = googleVerifier.verify(credential);
        if (!google.emailVerified()) {
            throw ApiException.unauthorized("GOOGLE_EMAIL_UNVERIFIED", "Your Google account's email isn't verified.");
        }
        String email = normalizeEmail(google.email());
        checkDomain(email);

        User user = userRepository.findByGoogleSubject(google.subject())
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .orElse(null);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setUsername(placeholderUsername());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // no password until they set one
            user.setRole(Role.USER);
            user.setNeedsUsername(true);
            log.info("New account via Google for {}", email);
        } else if (!user.hasVerifiedEmail()) {
            // A sign-up that was never verified: whoever started it never proved they own this inbox,
            // so its password (and chosen username) must not carry over to the real owner.
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setNeedsUsername(true);
        }
        user.setGoogleSubject(google.subject());
        user.setEmailVerified(true);
        userRepository.save(user);
        return startSession(user);
    }

    private AuthResponse startSession(User user) {
        adminBootstrap.apply(user);
        String sessionId = UUID.randomUUID().toString();
        user.setCurrentSessionId(sessionId); // invalidates tokens issued to any other device
        return tokenFor(user);
    }

    /** A fresh token for the user's current session (also used after a username change). */
    public AuthResponse tokenFor(User user) {
        JwtService.IssuedToken token = jwtService.issue(user.getId(), user.getUsername(), user.getCurrentSessionId());
        return new AuthResponse(token.token(), user.getUsername(), token.expiresAt(),
                user.mustChooseUsername(), user.effectiveRole().name());
    }

    private String placeholderUsername() {
        String name;
        do {
            byte[] bytes = new byte[5];
            random.nextBytes(bytes);
            name = Usernames.PLACEHOLDER_PREFIX + HexFormat.of().formatHex(bytes);
        } while (userRepository.existsByUsernameIgnoreCase(name));
        return name;
    }

    private void checkDomain(String email) {
        if (allowedEmailDomains.isEmpty()) {
            return;
        }
        String domain = email.substring(email.lastIndexOf('@') + 1);
        if (!allowedEmailDomains.contains(domain)) {
            throw ApiException.badRequest("EMAIL_DOMAIN_NOT_ALLOWED",
                    "Sign-up is limited to these email domains: " + String.join(", ", allowedEmailDomains));
        }
    }

    /** Server-side logout: the current token stops working immediately. */
    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(u -> u.setCurrentSessionId(null));
    }

    // ------------------------------------------------------------------ forgot password

    @Transactional(noRollbackFor = ApiException.class)
    public String forgotPassword(String rawEmail) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(rawEmail))
                .filter(User::hasVerifiedEmail)
                .ifPresent(u -> {
                    try {
                        otpService.issue(u, OtpPurpose.RESET_PASSWORD);
                    } catch (TooManyRequestsException e) {
                        // don't reveal anything; the previous code is still valid
                    }
                });
        return GENERIC_RESET_MESSAGE;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(User::hasVerifiedEmail)
                .orElseThrow(() -> ApiException.badRequest("OTP_INVALID", "Incorrect code."));
        otpService.verify(user, OtpPurpose.RESET_PASSWORD, request.code());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setCurrentSessionId(null); // sign out everywhere
        log.info("Password reset for '{}'", user.getUsername());
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
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
