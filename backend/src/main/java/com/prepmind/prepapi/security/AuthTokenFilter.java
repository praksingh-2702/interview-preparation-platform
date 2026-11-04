package com.prepmind.prepapi.security;

import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests that carry "Authorization: Bearer <jwt>".
 * It never short-circuits the request: if the token is bad, the request simply stays anonymous
 * (public endpoints like /login keep working) and the reason is left for the entry point,
 * which turns it into a JSON 401 for protected endpoints.
 */
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = AuthTokenFilter.class.getName() + ".error";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthTokenFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String jwt = extractBearer(request);
        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(jwt, request);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String jwt, HttpServletRequest request) {
        JwtService.TokenResult token = jwtService.parse(jwt);
        if (!token.isValid()) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, token.error() != null ? token.error() : AuthErrorCode.TOKEN_INVALID);
            return;
        }

        Optional<User> userOpt = token.userId() != null
                ? userRepository.findById(token.userId())
                : userRepository.findByUsername(token.username());
        if (userOpt.isEmpty()) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCode.TOKEN_INVALID);
            return;
        }
        User user = userOpt.get();

        // Single active session per account: a newer login (or a logout) invalidates older tokens.
        String activeSession = user.getCurrentSessionId();
        if (activeSession == null || !activeSession.equals(token.sessionId())) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCode.SESSION_REVOKED);
            return;
        }

        // Read from the database on every request, so a role change applies immediately.
        List<GrantedAuthority> authorities = user.effectiveRole().authorities().stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(user.getId(), user.getUsername()), null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static String extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
