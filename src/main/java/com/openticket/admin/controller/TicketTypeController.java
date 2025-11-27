package com.openticket.admin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openticket.admin.dto.TicketTypeDto;
import com.openticket.admin.entity.TicketType;
import com.openticket.admin.entity.User;
import com.openticket.admin.service.TicketTypeService;

@RestController
@RequestMapping("/api/tickets")
public class TicketTypeController {

    @Autowired
    private TicketTypeService service;

    private Long getCurrentUserId() {
        return 2L;
    }

    // 查全部票種（屬於當前主辦方的）
    @GetMapping
    public List<TicketTypeDto> getAll() {
        return service.getAllDtos();
    }

    @GetMapping("/for-event")
    public List<TicketTypeDto> getForEvent() {

        Long userId = getCurrentUserId();

        return service.getAllForOrganizer(userId).stream()
                .map(tt -> new TicketTypeDto(
                        tt.getId(),
                        tt.getName(),
                        tt.getPrice(),
                        tt.getIsLimited(),
                        tt.getLimitQuantity(),
                        tt.getDescription()))
                .toList();
    }

    // 取得票種模板
    @GetMapping("/templates")
    public List<TicketTypeDto> getTemplates() {
        return service.getTemplateDtos();
    }

    // （新增）主辦方自己的票種管理頁 → 只顯示自己的自訂票
    @GetMapping("/my")
    public List<TicketTypeDto> getMyTickets() {

        Long userId = getCurrentUserId();

        return service.getCustom(userId).stream()
                .map(tt -> new TicketTypeDto(
                        tt.getId(),
                        tt.getName(),
                        tt.getPrice(),
                        tt.getIsLimited(),
                        tt.getLimitQuantity(),
                        tt.getDescription()))
                .toList();
    }

    // 新增票種
    @PostMapping
    public TicketType create(@RequestBody TicketType tt) {
        // 這邊是硬資料，未來要引入登入驗證，取得用戶ID
        User user = new User();
        user.setId(getCurrentUserId());

        tt.setUser(user);
        tt.setIsDefault(true);
        return service.create(tt);
    }

    // 修改票種
    @PutMapping("/{id}")
    public TicketType update(
            @PathVariable Long id,
            @RequestBody TicketType tt) {
        return service.update(id, tt);
    }

    // 刪除票種
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/apply/{templateId}")
    public TicketType applyTemplate(@PathVariable Long templateId) {

        Long userId = getCurrentUserId();
        return service.applyTemplate(templateId, userId);
    }
}
