package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.AdminOverview;
import com.prepmind.prepapi.dto.AdminUserView;
import com.prepmind.prepapi.dto.RoleRequest;
import com.prepmind.prepapi.security.AuthenticatedUser;
import com.prepmind.prepapi.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** ADMIN only (see WebSecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public AdminOverview overview() {
        return adminService.overview();
    }

    @GetMapping("/users")
    public List<AdminUserView> users(@RequestParam(required = false) String q) {
        return adminService.users(q);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        adminService.deleteUser(actor.id(), id);
        return ResponseEntity.noContent().build();
    }

    /** Deletes accounts that never verified their email (older than 1 hour). */
    @PostMapping("/users/remove-unverified")
    public Map<String, Integer> removeUnverified() {
        return Map.of("removed", adminService.removeUnverified());
    }

    @PutMapping("/users/{id}/role")
    public AdminUserView changeRole(@AuthenticationPrincipal AuthenticatedUser actor,
                                    @PathVariable Long id,
                                    @Valid @RequestBody RoleRequest request) {
        return adminService.changeRole(actor.id(), id, request.role());
    }
}
