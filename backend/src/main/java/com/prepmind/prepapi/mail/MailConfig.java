package com.prepmind.prepapi.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    public EmailSender emailSender(@Value("${app.mail.brevo-api-key:}") String apiKey,
                                   @Value("${app.mail.from:}") String from,
                                   @Value("${app.mail.from-name:FreakCode Top 100}") String fromName) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("BREVO_API_KEY is not set: emails (OTP codes) will only be written to the log.");
            return new LoggingEmailSender();
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("MAIL_FROM must be set to a sender address verified in Brevo");
        }
        return new BrevoEmailSender(apiKey.trim(), from.trim(), fromName);
    }
}
