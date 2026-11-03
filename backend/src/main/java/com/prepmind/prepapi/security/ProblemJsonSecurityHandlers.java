package com.prepmind.prepapi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** JSON (problem+json) bodies for 401 and 403 so the frontend can react to a stable "code". */
public class ProblemJsonSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object attr = request.getAttribute(AuthTokenFilter.AUTH_ERROR_ATTRIBUTE);
        AuthErrorCode code = attr instanceof AuthErrorCode c ? c : AuthErrorCode.AUTH_REQUIRED;
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", code.name(), code.message());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "FORBIDDEN",
                "You do not have permission to do that.");
    }

    // Messages are fixed constants without quotes or backslashes, so no JSON escaping is needed.
    private static void write(HttpServletResponse response, int status, String title, String code, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\",\"code\":\"" + code + "\"}");
    }
}
