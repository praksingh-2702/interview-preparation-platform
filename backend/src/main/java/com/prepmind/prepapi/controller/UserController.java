package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.AuthResponse;
import com.prepmind.prepapi.dto.PreferencesRequest;
import com.prepmind.prepapi.dto.UserResponse;
import com.prepmind.prepapi.dto.UsernameAvailability;
import com.prepmind.prepapi.dto.UsernameRequest;
import com.prepmind.prepapi.security.AuthenticatedUser;
import com.prepmind.prepapi.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AccountService accountService;

    public UserController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return accountService.me(user.id());
    }

    /** Public (the sign-up form uses it too). Signed-in callers may keep their own current name. */
    @GetMapping("/username-available")
    public UsernameAvailability usernameAvailable(@RequestParam String username,
                                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return accountService.availability(username, user == null ? null : user.id());
    }

    @PutMapping("/me/username")
    public AuthResponse changeUsername(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody UsernameRequest request) {
        return accountService.changeUsername(user.id(), request.username());
    }

    @PutMapping("/me/preferences")
    public UserResponse preferences(@AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid @RequestBody PreferencesRequest request) {
        return accountService.setLeaderboardVisibility(user.id(), request.showOnLeaderboard());
    }
}
