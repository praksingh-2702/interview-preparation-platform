package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.LeaderboardResponse;
import com.prepmind.prepapi.security.AuthenticatedUser;
import com.prepmind.prepapi.service.LeaderboardService;
import com.prepmind.prepapi.service.Zones;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /** tz: the viewer's IANA timezone, so streaks line up with their day. */
    @GetMapping
    public LeaderboardResponse leaderboard(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam(required = false) String tz,
                                           @RequestParam(defaultValue = "50") int limit) {
        return leaderboardService.leaderboard(user.id(), Zones.parse(tz), limit);
    }
}
