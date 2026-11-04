package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.EmailOtp;
import com.prepmind.prepapi.entity.OtpPurpose;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.exception.TooManyRequestsException;
import com.prepmind.prepapi.mail.EmailSender;
import com.prepmind.prepapi.repository.EmailOtpRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 6-digit email codes: valid 10 minutes, 5 wrong guesses max, 60 s between sends, 5 sends per hour.
 * A new code replaces any older unused one for the same purpose.
 */
@Service
public class OtpService {

    static final Duration TTL = Duration.ofMinutes(10);
    static final int MAX_ATTEMPTS = 5;
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_PER_HOUR = 5;

    private final EmailOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpService(EmailOtpRepository otpRepository, PasswordEncoder passwordEncoder,
                      EmailSender emailSender, Clock clock) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    /**
     * noRollbackFor: callers catch the cooldown exception and carry on, so it must not mark their
     * transaction rollback-only.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void issue(User user, OtpPurpose purpose) {
        Instant now = clock.instant();

        Optional<EmailOtp> latest = otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose);
        if (latest.isPresent()) {
            Instant nextAllowed = latest.get().getCreatedAt().plus(RESEND_COOLDOWN);
            if (now.isBefore(nextAllowed)) {
                long wait = Math.max(1, Duration.between(now, nextAllowed).toSeconds());
                throw new TooManyRequestsException("OTP_COOLDOWN",
                        "Please wait " + wait + " seconds before requesting another code.", wait);
            }
        }
        if (otpRepository.countByUserIdAndPurposeAndCreatedAtAfter(user.getId(), purpose, now.minus(Duration.ofHours(1)))
                >= MAX_PER_HOUR) {
            throw new TooManyRequestsException("OTP_LIMIT",
                    "Too many codes requested. Please try again in an hour.", 3600);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        // Send first: if the email fails, nothing is stored and the user can retry immediately.
        sendEmail(user, purpose, code);

        otpRepository.consumeAllActive(user.getId(), purpose, now);
        EmailOtp otp = new EmailOtp();
        otp.setUser(user);
        otp.setPurpose(purpose);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plus(TTL));
        otp.setAttempts(0);
        otpRepository.save(otp);
    }

    /** Throws OTP_INVALID / OTP_EXPIRED; failed attempts are counted even though an exception is thrown. */
    @Transactional(noRollbackFor = ApiException.class)
    public void verify(User user, OtpPurpose purpose, String code) {
        Instant now = clock.instant();
        EmailOtp otp = otpRepository.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> ApiException.badRequest("OTP_EXPIRED", "That code has expired. Request a new one."));

        if (!now.isBefore(otp.getExpiresAt()) || otp.getAttempts() >= MAX_ATTEMPTS) {
            otp.setConsumedAt(now);
            throw ApiException.badRequest("OTP_EXPIRED", "That code has expired. Request a new one.");
        }
        if (code == null || !passwordEncoder.matches(code.trim(), otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            int left = MAX_ATTEMPTS - otp.getAttempts();
            if (left <= 0) {
                otp.setConsumedAt(now);
                throw ApiException.badRequest("OTP_EXPIRED", "Too many wrong attempts. Request a new code.");
            }
            throw ApiException.badRequest("OTP_INVALID",
                    "Incorrect code. " + left + (left == 1 ? " attempt" : " attempts") + " left.");
        }
        otp.setConsumedAt(now);
    }

    private void sendEmail(User user, OtpPurpose purpose, String code) {
        boolean verify = purpose == OtpPurpose.VERIFY_EMAIL;
        String subject = verify
                ? code + " is your FreakCode verification code"
                : code + " is your FreakCode password reset code";
        String action = verify ? "verify your email" : "reset your password";
        String text = "Hi " + user.getUsername() + ",\n\n"
                + "Your code to " + action + " is: " + code + "\n\n"
                + "It expires in 10 minutes. If you didn't request this, you can ignore this email.\n\n"
                + "- FreakCode Top 100";
        String html = "<div style=\"font-family:-apple-system,Segoe UI,Arial,sans-serif;max-width:480px;margin:auto;"
                + "padding:24px;background:#0d1117;color:#c9d1d9;border-radius:12px\">"
                + "<h2 style=\"color:#f0f6fc;margin:0 0 12px\">FreakCode <span style=\"color:#58a6ff;font-weight:400\">Top 100</span></h2>"
                + "<p>Hi " + escape(user.getUsername()) + ",</p>"
                + "<p>Your code to " + action + " is:</p>"
                + "<p style=\"font-size:32px;font-weight:700;letter-spacing:8px;color:#f0f6fc;margin:16px 0\">" + code + "</p>"
                + "<p style=\"color:#8b949e;font-size:13px\">It expires in 10 minutes. If you didn't request this, ignore this email.</p>"
                + "</div>";
        emailSender.send(user.getEmail(), subject, text, html);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
