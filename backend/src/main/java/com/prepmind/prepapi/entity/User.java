package com.prepmind.prepapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * The one session that is currently allowed to use this account. Rotated on every login and
     * cleared on logout, so older tokens stop working immediately.
     */
    @Column(name = "current_session_id")
    private String currentSessionId;

    /**
     * USER, EDITOR or ADMIN. Null for accounts created before v3 (treated as USER). Changed only by an
     * admin, or granted at startup/login to the usernames in app.admin.usernames.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Role role;

    /** Google account id ("sub"), set once the user has signed in with Google. */
    @Column(name = "google_subject", unique = true)
    private String googleSubject;

    /** Null (pre-v3) or true = shown on the leaderboard. */
    @Column(name = "show_on_leaderboard")
    private Boolean showOnLeaderboard;

    /** True right after a first Google sign-in, until the user picks their username. */
    @Column(name = "needs_username")
    private Boolean needsUsername;

    @Column(name = "created_at")
    private Instant createdAt;

    /** Null for accounts created before OTP verification existed; treated as not verified. */
    @Column(name = "email_verified")
    private Boolean emailVerified;

    public boolean hasVerifiedEmail() {
        return Boolean.TRUE.equals(emailVerified);
    }

    public Role effectiveRole() {
        return role == null ? Role.USER : role;
    }

    public boolean isListedOnLeaderboard() {
        return !Boolean.FALSE.equals(showOnLeaderboard);
    }

    public boolean mustChooseUsername() {
        return Boolean.TRUE.equals(needsUsername);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
