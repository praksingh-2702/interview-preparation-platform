package com.prepmind.prepapi.google;

import com.prepmind.prepapi.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies Google ID tokens locally: RS256 signature against Google's published keys (cached per
 * their Cache-Control), issuer, audience (our client id), expiry. See
 * https://developers.google.com/identity/gsi/web/guides/verify-google-id-token
 */
@Component
public class GoogleJwksTokenVerifier implements GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleJwksTokenVerifier.class);
    private static final String CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)");
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration MIN_REFRESH_GAP = Duration.ofMinutes(1);

    private final String clientId;
    private final RestClient http;
    private volatile Map<String, PublicKey> keys = Map.of();
    private volatile Instant keysExpireAt = Instant.EPOCH;
    private volatile Instant lastFetch = Instant.EPOCH;

    public GoogleJwksTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.http = RestClient.create();
    }

    @Override
    public boolean isEnabled() {
        return !clientId.isEmpty();
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                        @Override
                        public Key resolveSigningKey(JwsHeader header, Claims body) {
                            return keyFor(header.getKeyId());
                        }
                    })
                    .requireAudience(clientId)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();
            if (!ISSUERS.contains(claims.getIssuer())) {
                throw invalid();
            }
            String email = claims.get("email", String.class);
            Object verified = claims.get("email_verified");
            boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equals(verified);
            if (claims.getSubject() == null || email == null) {
                throw invalid();
            }
            return new GoogleIdentity(claims.getSubject(), email, emailVerified, claims.get("name", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected Google token: {}", e.getMessage());
            throw invalid();
        }
    }

    private Key keyFor(String kid) {
        PublicKey key = kid == null ? null : currentKeys().get(kid);
        if (key == null && Instant.now().isAfter(lastFetch.plus(MIN_REFRESH_GAP))) {
            key = refresh().get(kid); // Google rotated its keys since we cached them
        }
        if (key == null) {
            throw invalid();
        }
        return key;
    }

    private Map<String, PublicKey> currentKeys() {
        return Instant.now().isBefore(keysExpireAt) ? keys : refresh();
    }

    @SuppressWarnings("unchecked")
    private synchronized Map<String, PublicKey> refresh() {
        if (Instant.now().isBefore(keysExpireAt) && Instant.now().isBefore(lastFetch.plus(MIN_REFRESH_GAP))) {
            return keys; // another thread just refreshed
        }
        lastFetch = Instant.now();
        try {
            ResponseEntity<Map> response = http.get().uri(CERTS_URL).retrieve().toEntity(Map.class);
            Map<String, PublicKey> fresh = new HashMap<>();
            Object list = response.getBody() == null ? null : response.getBody().get("keys");
            if (list instanceof List<?> jwks) {
                for (Object o : jwks) {
                    if (o instanceof Map<?, ?> jwk && "RSA".equals(jwk.get("kty"))) {
                        fresh.put(String.valueOf(jwk.get("kid")), rsaKey((String) jwk.get("n"), (String) jwk.get("e")));
                    }
                }
            }
            keys = Map.copyOf(fresh);
            keysExpireAt = Instant.now().plus(ttl(response.getHeaders().getCacheControl()));
        } catch (RestClientException | GeneralSecurityException | ClassCastException e) {
            log.warn("Could not fetch Google signing keys: {}", e.getMessage());
            if (keys.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "GOOGLE_UNAVAILABLE",
                        "Couldn't reach Google to verify your sign-in. Please try again.");
            }
        }
        return keys;
    }

    private static PublicKey rsaKey(String n, String e) throws GeneralSecurityException {
        Base64.Decoder b64 = Base64.getUrlDecoder();
        return KeyFactory.getInstance("RSA").generatePublic(
                new RSAPublicKeySpec(new BigInteger(1, b64.decode(n)), new BigInteger(1, b64.decode(e))));
    }

    private static Duration ttl(String cacheControl) {
        if (cacheControl != null) {
            Matcher m = MAX_AGE.matcher(cacheControl);
            if (m.find()) {
                return Duration.ofSeconds(Math.max(60, Long.parseLong(m.group(1))));
            }
        }
        return DEFAULT_TTL;
    }

    private static ApiException invalid() {
        return ApiException.unauthorized("GOOGLE_TOKEN_INVALID", "Google sign-in failed. Please try again.");
    }
}
