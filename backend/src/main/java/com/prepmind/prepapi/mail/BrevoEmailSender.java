package com.prepmind.prepapi.mail;

import com.prepmind.prepapi.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends mail through Brevo's HTTP API (https://developers.brevo.com/reference/sendtransacemail).
 * HTTP rather than SMTP because Render's free instances block outbound SMTP ports.
 */
public class BrevoEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailSender.class);

    private final RestClient client;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailSender(String apiKey, String fromEmail, String fromName) {
        this.client = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void send(String to, String subject, String text, String html) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromEmail, "name", fromName),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", text,
                "htmlContent", html);
        try {
            client.post().uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Brevo send to {} failed: {}", to, e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EMAIL_FAILED",
                    "We couldn't send the email right now. Please try again in a minute.");
        }
    }
}
