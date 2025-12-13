package com.openticket.admin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openticket.admin.entity.Announcement;
import com.openticket.admin.entity.Role;
import com.openticket.admin.repository.AnnoRepository;

@Service
public class AnnouncementService {

    @Autowired
    private AnnoRepository repository;

    // 查詢所有公告
    public List<Announcement> getAllForUser(Long companyId, Role role) {

        if (role == Role.ADMIN) {
            return repository.findAdminAnnouncements();
            // return repository.findAll();
        }

        return repository.findByUserId(companyId);
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