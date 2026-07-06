package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.entity.Progress;
import com.prepmind.prepapi.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List; // <-- Make sure this is here!
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    @Autowired
    private ProgressService progressService;

    // POST request to update or create progress
    @PostMapping("/update/{questionId}")
    public ResponseEntity<?> updateProgress(
            @PathVariable Long questionId,
            @RequestBody Map<String, String> requestBody,
            Principal principal) {
        
        // Extract the username directly from the verified JWT Token
        String username = principal.getName();
        String status = requestBody.get("status"); // Expected: "SOLVED" or "ATTEMPTED"

        try {
            Progress updatedProgress = progressService.markQuestionProgress(username, questionId, status);
            return ResponseEntity.ok(updatedProgress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET request to fetch the user's dashboard (The missing piece!)
    @GetMapping("/my-progress")
    public ResponseEntity<?> getMyProgress(Principal principal) {
        String username = principal.getName();
        try {
            List<Progress> myProgress = progressService.getUserProgress(username);
            return ResponseEntity.ok(myProgress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}