package com.prepmind.prepapi.google;

/** Verifies the ID token from "Sign in with Google". Swapped for a fake in tests. */
public interface GoogleTokenVerifier {

    /** False when GOOGLE_CLIENT_ID isn't configured; the sign-in endpoint then reports it as disabled. */
    boolean isEnabled();

    /** @throws com.prepmind.prepapi.exception.ApiException 401 GOOGLE_TOKEN_INVALID if anything is off */
    GoogleIdentity verify(String idToken);
}
