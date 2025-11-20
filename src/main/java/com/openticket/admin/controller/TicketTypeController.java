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

    // 查全部票種（屬於當前主辦方的）
    @GetMapping
    public List<TicketTypeDto> getAll() {
        return service.getAllDtos();
    }

    // 新增票種
    @PostMapping
    public TicketType create(@RequestBody TicketType tt) {
        // 這邊是硬資料，未來要引入登入驗證，取得用戶ID
        User user = new User();
        user.setId(2L);
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
}
