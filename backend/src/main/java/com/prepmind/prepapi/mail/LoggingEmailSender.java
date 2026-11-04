package com.prepmind.prepapi.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used when no email provider is configured (local dev and tests): the email is written to the log
 * instead of being sent, and the last message per address is kept so tests can read the code.
 */
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);
    private static final int MAX_KEPT = 1000;

    private final Map<String, String> lastMessage = new ConcurrentHashMap<>();

    @Override
    public void send(String to, String subject, String text, String html) {
        log.warn("EMAIL NOT SENT (set BREVO_API_KEY to enable). To: {} | {} | {}", to, subject, text.replace('\n', ' '));
        if (lastMessage.size() > MAX_KEPT) {
            lastMessage.clear();
        }
        lastMessage.put(to.toLowerCase(Locale.ROOT), text);
    }

    public String lastMessageTo(String email) {
        return lastMessage.get(email.toLowerCase(Locale.ROOT));
    }
}
