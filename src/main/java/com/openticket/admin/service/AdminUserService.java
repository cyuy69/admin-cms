package com.openticket.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.openticket.admin.entity.User;
import com.openticket.admin.repository.AdminUserRepository;

@Service
public class AdminUserService {

    private final AdminUserRepository repo;

    public AdminUserService(AdminUserRepository repo) {
        this.repo = repo;
    }

    public Page<User> searchUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repo.searchUsers(keyword, pageable);
    }

    public void updateRole(Long userId, int role) {
        User u = repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setRole(role);
        repo.save(u);
    }

    public void updateActive(Long userId, int active) {
        User u = repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setIsActive(active == 1);
        repo.save(u);
    }
}
