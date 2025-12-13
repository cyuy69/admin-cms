package com.openticket.admin.controller.organizer;

import java.util.List;
import java.util.Map;

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

import com.openticket.admin.dto.TicketTypeDto;
import com.openticket.admin.entity.TicketType;
import com.openticket.admin.security.LoginCompanyProvider;
import com.openticket.admin.service.TicketTypeService;

@RestController
@RequestMapping("/api/tickets")
public class TicketTypeController {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields(
                "id",
                "user",
                "userId",
                "createdAt",
                "isDefault");
    }

    @Autowired
    private TicketTypeService service;

    @Autowired
    private LoginCompanyProvider loginCompanyProvider;

    // 查全部票種（屬於當前主辦方的）
    @GetMapping
    public List<TicketTypeDto> getAll() {
        return service.getAllDtos();
    }

    @GetMapping("/for-event")
    public List<TicketTypeDto> getForEvent() {

        Long companyId = loginCompanyProvider.getCompanyId();

        return service.getAllForOrganizer(companyId).stream()
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

    // 取得單一票種（編輯用）
    @GetMapping("/{id}")
    public TicketTypeDto getOne(@PathVariable Long id) {
        // 之後可以加上「只能看自己的」的判斷
        return service.getOneDto(id);
    }

    // 主辦方自己的票種管理頁 → 只顯示自己的自訂票
    @GetMapping("/my")
    public List<TicketTypeDto> getMyTickets() {

        Long companyId = loginCompanyProvider.getCompanyId();

        return service.getCustom(companyId).stream()
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
        Long companyId = loginCompanyProvider.getCompanyId();
        return service.create(tt, companyId);
    }

    // 修改票種
    @PutMapping("/{id}")
    public TicketTypeDto update(@PathVariable Long id, @RequestBody TicketType tt) {
        return service.updateDto(id, tt);
    }

    // 刪除票種
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("success", true);
    }

    @PostMapping("/apply/{templateId}")
    public TicketType applyTemplate(@PathVariable Long templateId) {

        Long companyId = loginCompanyProvider.getCompanyId();
        return service.applyTemplate(templateId, companyId);
    }
}
