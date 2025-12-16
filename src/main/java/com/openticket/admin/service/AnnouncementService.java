package com.openticket.admin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.openticket.admin.entity.Announcement;
import com.openticket.admin.entity.Role;
import com.openticket.admin.repository.AnnoRepository;

@Service
public class AnnouncementService {

    @Autowired
    private AnnoRepository repository;

    // 查詢所有公告
    public Page<Announcement> getAllForUser(Long companyId, Role role, Pageable pageable) {
        if (role == Role.ADMIN) {
            return repository.findAdminAnnouncements(pageable);
        }
        return repository.findByUserId(companyId, pageable);
    }

    public Page<Announcement> searchByKeyword(String keyword, Pageable pageable) {
        return repository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
    }

    // 新增公告
    public Announcement create(Announcement ann) {
        if (ann == null) {
            throw new IllegalArgumentException("公告資料不能為 null");
        }
        return repository.save(ann);
    }

    // 公告數量
    public long count() {
        return repository.count();
    }

    public Announcement getById(Long id) {
        if (id == null)
            return null;
        return repository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

}