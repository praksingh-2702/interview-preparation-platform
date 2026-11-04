package com.prepmind.prepapi.dto;

import java.time.Instant;

/** One row of the admin "Users" table. completed = rows ticked Solved / Read; points as on the leaderboard. */
public record AdminUserView(Long id,
                            String username,
                            String email,
                            String role,
                            boolean verified,
                            boolean google,
                            boolean showOnLeaderboard,
                            Instant createdAt,
                            long completed,
                            long points) {
}
