package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.LoginRequest;
import com.prepmind.prepapi.dto.RegisterRequest;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.repository.UserRepository;
import com.prepmind.prepapi.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    public String registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return "Error: Username is already taken!";
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return "Error: Email is already in use!";
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        if (registerRequest.getRoles() == null || registerRequest.getRoles().isEmpty()) {
            HashSet<String> defaultRoles = new HashSet<>();
            defaultRoles.add("ROLE_USER");
            user.setRoles(defaultRoles);
        } else {
            user.setRoles(registerRequest.getRoles());
        }

        userRepository.save(user);
        return "User registered successfully!";
    }

    public Optional<String> authenticateUser(LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        
        if (userOpt.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            
            // 1. Generate a brand new cryptographically secure session UUID
            String dynamicSessionId = UUID.randomUUID().toString();
            
            // 2. Persist the session ID to the user record (overwrites old logins)
            user.setCurrentSessionId(dynamicSessionId);
            userRepository.save(user);
            
            // 3. Build the JWT by binding both username and our fresh sessionId claim
            String token = jwtUtils.generateJwtToken(user.getUsername(), dynamicSessionId);
            return Optional.of(token);
        }
        return Optional.empty();
    }
}