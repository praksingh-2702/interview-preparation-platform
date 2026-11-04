package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.ProgressRequest;
import com.prepmind.prepapi.dto.ProgressStatus;
import com.prepmind.prepapi.dto.ProgressUpdateRequest;
import com.prepmind.prepapi.dto.ReviewRequest;
import com.prepmind.prepapi.security.AuthenticatedUser;
import com.prepmind.prepapi.service.ProgressService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /** { "<questionId>": { "solved": true, "revised": false }, ... } */
    @GetMapping("/my-status")
    public Map<Long, ProgressStatus> myStatus(@AuthenticationPrincipal AuthenticatedUser user) {
        return progressService.statusMap(user.id());
    }

    @PutMapping("/{questionId}")
    public ProgressStatus update(@AuthenticationPrincipal AuthenticatedUser user,
                                 @PathVariable Long questionId,
                                 @RequestBody ProgressUpdateRequest request) {
        return progressService.update(user.id(), questionId, request.solved(), request.revised());
    }

    /** Revision queue: GOOD pushes the next review out (1→3→7→14→30→60 days), AGAIN resets to 1 day. */
    @PostMapping("/{questionId}/review")
    public ProgressStatus review(@AuthenticationPrincipal AuthenticatedUser user,
                                 @PathVariable Long questionId,
                                 @Valid @RequestBody ReviewRequest request) {
        return progressService.review(user.id(), questionId, request.result());
    }

    /** Kept for older frontends; prefer PUT /api/progress/{questionId}. */
    @PostMapping("/toggle")
    public ProgressStatus toggle(@AuthenticationPrincipal AuthenticatedUser user,
                                 @Valid @RequestBody ProgressRequest request) {
        return progressService.update(user.id(), request.questionId(), request.solved(), request.revised());
    }
}
