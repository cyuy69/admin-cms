package com.openticket.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openticket.admin.entity.Announcement;
import com.openticket.admin.entity.Role;
import com.openticket.admin.entity.User;
import com.openticket.admin.security.LoginCompanyProvider;
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

    @Autowired
    private LoginCompanyProvider loginCompanyProvider;

    // 取得全部公告
    @GetMapping
    public Page<Announcement> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        Long companyId = loginCompanyProvider.getCompanyId();
        Role role = loginCompanyProvider.getRole();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (keyword == null || keyword.isEmpty()) {
            return service.getAllForUser(companyId, role, pageable);
        } else {
            // 這裡直接呼叫 repository 的分頁查詢
            return service.searchByKeyword(keyword, pageable);
        }
    }

    // 新增公告
    @PostMapping
    public Announcement create(@RequestBody Announcement ann) {
        Long companyId = loginCompanyProvider.getCompanyId();
        // 這邊是硬資料，未來要引入登入驗證，取得用戶ID
        User user = new User();
        user.setId(companyId);
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
