package com.prepmind.prepapi.security;

import java.security.Principal;

/** The principal stored in the SecurityContext for an authenticated request. */
public record AuthenticatedUser(Long id, String username) implements Principal {
    @Override
    public String getName() {
        return username;
    }
}
