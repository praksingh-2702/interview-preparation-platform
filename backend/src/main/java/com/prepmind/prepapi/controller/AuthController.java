package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.AuthResponse;
import com.prepmind.prepapi.dto.EmailRequest;
import com.prepmind.prepapi.dto.GoogleLoginRequest;
import com.prepmind.prepapi.dto.LoginRequest;
import com.prepmind.prepapi.dto.MessageResponse;
import com.prepmind.prepapi.dto.RegisterRequest;
import com.prepmind.prepapi.dto.RegisterResponse;
import com.prepmind.prepapi.dto.ResetPasswordRequest;
import com.prepmind.prepapi.dto.VerifyEmailRequest;
import com.prepmind.prepapi.security.AuthenticatedUser;
import com.prepmind.prepapi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** register → verify-email → login; google; forgot-password → reset-password. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return new MessageResponse("Email verified. You can now log in.");
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request.email());
        return new MessageResponse("If that account is waiting for verification, a new code is on its way.");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest.getRemoteAddr());
    }

    /** Sign in with Google: the browser sends the ID token from Google Identity Services. */
    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.googleLogin(request.credential());
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        return new MessageResponse(authService.forgotPassword(request.email()));
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Password updated. Log in with your new password.");
    }

    /** Public, so a client holding an already-expired token can still log out without an error. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user != null) {
            authService.logout(user.id());
        }
        return ResponseEntity.noContent().build();
    }
}
