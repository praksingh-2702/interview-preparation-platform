package com.prepmind.prepapi.entity;

/**
 * USER   tracks their own progress.
 * EDITOR also adds, edits and deletes questions.
 * ADMIN  also manages users' roles and sees the admin overview.
 */
public enum Role {
    USER, EDITOR, ADMIN;

    /** Spring Security authorities, each role including the ones below it. */
    public java.util.List<String> authorities() {
        return switch (this) {
            case USER -> java.util.List.of("ROLE_USER");
            case EDITOR -> java.util.List.of("ROLE_USER", "ROLE_EDITOR");
            case ADMIN -> java.util.List.of("ROLE_USER", "ROLE_EDITOR", "ROLE_ADMIN");
        };
    }
}
