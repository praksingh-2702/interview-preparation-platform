package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.AuthResponse;
import com.prepmind.prepapi.dto.LoginRequest;
import com.prepmind.prepapi.dto.MessageResponse;
import com.prepmind.prepapi.dto.RegisterRequest;
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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Account created. You can now log in."));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest.getRemoteAddr());
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
