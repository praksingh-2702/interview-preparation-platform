package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.JwtResponse;
import com.prepmind.prepapi.dto.LoginRequest;
import com.prepmind.prepapi.dto.RegisterRequest;
import com.prepmind.prepapi.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "http://localhost:3000") // Unlocks connection from frontend
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        String result = authService.registerUser(registerRequest);
        if (result.startsWith("Error:")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Optional<String> tokenOpt = authService.authenticateUser(loginRequest);
        if (tokenOpt.isPresent()) {
            return ResponseEntity.ok(new JwtResponse(tokenOpt.get(), loginRequest.getUsername()));
        }
        return ResponseEntity.status(401).body("Error: Invalid username or password!");
    }
}