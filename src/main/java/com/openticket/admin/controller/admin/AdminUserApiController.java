package com.openticket.admin.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openticket.admin.entity.User;
import com.openticket.admin.service.AdminUserService;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserApiController {

    private final AdminUserService service;

    public AdminUserApiController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public Page<User> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.searchUsers(keyword, page, size);
    }

    @PutMapping("/{id}/role")
    public void updateRole(
            @PathVariable Long id,
            @RequestParam int role) {
        service.updateRole(id, role);
    }

    @PutMapping("/{id}/active")
    public void updateActive(
            @PathVariable Long id,
            @RequestParam int active) {
        service.updateActive(id, active);
    }
}
