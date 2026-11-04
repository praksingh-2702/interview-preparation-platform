package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.AdminOverview;
import com.prepmind.prepapi.dto.AdminUserView;
import com.prepmind.prepapi.dto.RoleRequest;
import com.prepmind.prepapi.security.AuthenticatedUser;
import com.prepmind.prepapi.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PutMapping("/users/{id}/role")
    public AdminUserView changeRole(@AuthenticationPrincipal AuthenticatedUser actor,
                                    @PathVariable Long id,
                                    @Valid @RequestBody RoleRequest request) {
        return adminService.changeRole(actor.id(), id, request.role());
    }
}
