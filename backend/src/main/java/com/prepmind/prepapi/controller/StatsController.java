package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.StatsResponse;
import com.prepmind.prepapi.security.AuthenticatedUser;
import com.prepmind.prepapi.service.StatsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public StatsResponse stats(@AuthenticationPrincipal AuthenticatedUser user,
                               @RequestParam(required = false) String tz) {
        return statsService.stats(user.id(), tz);
    }
}
