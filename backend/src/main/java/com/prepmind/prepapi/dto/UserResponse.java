package com.prepmind.prepapi.dto;

import com.prepmind.prepapi.entity.User;

public record UserResponse(Long id,
                           String username,
                           String email,
                           String role,
                           boolean showOnLeaderboard,
                           boolean needsUsername,
                           boolean googleLinked) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.effectiveRole().name(),
                u.isListedOnLeaderboard(), u.mustChooseUsername(), u.getGoogleSubject() != null);
    }
}
