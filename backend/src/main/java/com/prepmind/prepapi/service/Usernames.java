package com.prepmind.prepapi.service;

import com.prepmind.prepapi.exception.ApiException;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** One set of username rules for registration, the Google username picker and the live availability check. */
public final class Usernames {

    public static final int MIN = 3;
    public static final int MAX = 30;
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9_.-]+$");
    /** Placeholder names given to new Google accounts until they pick one. */
    static final String PLACEHOLDER_PREFIX = "google-";

    private Usernames() {
    }

    /** Empty if the format is fine, otherwise why it isn't. Uniqueness is checked separately. */
    public static Optional<String> formatProblem(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.length() < MIN || name.length() > MAX) {
            return Optional.of("Username must be " + MIN + "-" + MAX + " characters.");
        }
        if (!ALLOWED.matcher(name).matches()) {
            return Optional.of("Use only letters, digits, '.', '_' and '-'.");
        }
        if (name.toLowerCase(Locale.ROOT).startsWith(PLACEHOLDER_PREFIX)) {
            return Optional.of("Usernames can't start with '" + PLACEHOLDER_PREFIX + "'.");
        }
        return Optional.empty();
    }

    public static String requireValid(String raw) {
        formatProblem(raw).ifPresent(problem -> {
            throw ApiException.badRequest("USERNAME_INVALID", problem).with("errors", java.util.Map.of("username", problem));
        });
        return raw.trim();
    }

    /** A suggestion for the picker, from the email's local part: "Om.Sanjeet+x@gmail.com" -> "om.sanjeet". */
    public static String suggestionFromEmail(String email) {
        String local = email == null ? "" : email.split("@", 2)[0];
        local = local.split("\\+", 2)[0].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "");
        if (local.length() > MAX) {
            local = local.substring(0, MAX);
        }
        while (local.length() < MIN) {
            local = local + "_";
        }
        return local;
    }
}
