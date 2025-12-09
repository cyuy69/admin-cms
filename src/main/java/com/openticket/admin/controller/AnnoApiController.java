package com.openticket.admin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openticket.admin.entity.Announcement;
import com.openticket.admin.entity.User;
import com.openticket.admin.service.AnnouncementService;

@RestController
@RequestMapping("/api/announcements")
public class AnnoApiController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields(
                "id",
                "createdAt",
                "user",
                "userId");
    }

    @Autowired
    private AnnouncementService service;

    // 取得全部公告
    @GetMapping
    public List<Announcement> getAll() {
        return service.getAll();
    }

    // 新增公告
    @PostMapping
    public Announcement create(@RequestBody Announcement ann) {

        // 這邊是硬資料，未來要引入登入驗證，取得用戶ID
        User user = new User();
        user.setId(2L);
        ann.setUser(user);

        return service.create(ann);
    }

    // 更新公告
    @PutMapping("/{id}")
    public Announcement update(
            @PathVariable Long id,
            @RequestBody Announcement ann) {

        Announcement existing = service.getById(id);
        if (existing == null) {
            throw new RuntimeException("公告不存在 id = " + id);
        }

        existing.setTitle(ann.getTitle());
        existing.setContent(ann.getContent());

        return service.create(existing);
    }

    // 刪除公告
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
