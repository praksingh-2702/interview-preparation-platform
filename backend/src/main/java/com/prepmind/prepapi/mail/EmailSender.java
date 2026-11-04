package com.prepmind.prepapi.mail;

public interface EmailSender {

    /** Sends one email; throws ApiException(EMAIL_FAILED) if delivery could not be started. */
    void send(String to, String subject, String text, String html);
}
