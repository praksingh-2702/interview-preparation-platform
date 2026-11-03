package com.prepmind.prepapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String SESSION_CLAIM = "sessionId";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set to a random value of at least 32 characters (try: openssl rand -base64 48)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public IssuedToken issue(String username, String sessionId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expirationMs);
        String token = Jwts.builder()
                .setSubject(username)
                .claim(SESSION_CLAIM, sessionId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /** Parses and verifies the token once; never throws. */
    public TokenResult parse(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            return TokenResult.valid(claims.getSubject(), claims.get(SESSION_CLAIM, String.class));
        } catch (ExpiredJwtException e) {
            return TokenResult.invalid(AuthErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return TokenResult.invalid(AuthErrorCode.TOKEN_INVALID);
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public record TokenResult(String username, String sessionId, AuthErrorCode error) {
        static TokenResult valid(String username, String sessionId) {
            return new TokenResult(username, sessionId, null);
        }

        static TokenResult invalid(AuthErrorCode error) {
            return new TokenResult(null, null, error);
        }

        public boolean isValid() {
            return error == null && username != null;
        }
    }
}
