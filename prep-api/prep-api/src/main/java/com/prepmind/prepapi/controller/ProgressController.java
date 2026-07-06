package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.ProgressRequest;
import com.prepmind.prepapi.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    @Autowired
    private ProgressService progressService;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleProgress(
            @RequestBody ProgressRequest request, 
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            progressService.updateProgress(
                userDetails.getUsername(), 
                request.getQuestionId(), 
                request.isSolved(), 
                request.isRevised()
            );
            return ResponseEntity.ok("Progress states updated successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // NEW ENDPOINT: Feeds the UI exactly what it needs to initialize checkbox checkmarks
    @GetMapping("/my-status")
    public ResponseEntity<?> getMyStatus(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<Long, Map<String, Boolean>> statusMap = 
                progressService.getUserProgressMap(userDetails.getUsername());
            return ResponseEntity.ok(statusMap);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}