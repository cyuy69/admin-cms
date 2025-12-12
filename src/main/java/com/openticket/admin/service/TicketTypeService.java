package com.openticket.admin.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openticket.admin.dto.TicketTypeDto;
import com.openticket.admin.entity.TicketType;
import com.openticket.admin.entity.User;
import com.openticket.admin.repository.TicketTypeRepository;

@Service
public class TicketTypeService {

    @Autowired
    private TicketTypeRepository repo;

    public List<TicketType> getAll() {
        return repo.findAll();
    }

    public TicketType create(TicketType tt, Long userId) {

        User user = new User();
        user.setId(userId);
        tt.setUser(user);
        tt.setIsDefault(true);

        return repo.save(tt);
    }

    // 取得「系統模板」
    public List<TicketType> getTemplates() {
        return repo.findByIsDefaultFalse();
    }

    // 取得某主辦方的自訂票
    public List<TicketType> getCustom(Long userId) {
        return repo.findByIsDefaultTrueAndUserId(userId);
    }

    // 活動用 → 模板 + 自訂票
    public List<TicketType> getAllForOrganizer(Long userId) {
        List<TicketType> result = new ArrayList<>();
        result.addAll(getTemplates());
        result.addAll(getCustom(userId));
        return result;
    }

    public TicketType update(Long id, TicketType newData) {
        TicketType tt = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("票種不存在 ID=" + id));

        if (Boolean.FALSE.equals(tt.getIsDefault())) {
            throw new RuntimeException("系統模板不可修改");
        }

        tt.setName(newData.getName());
        tt.setPrice(newData.getPrice());
        tt.setIsLimited(newData.getIsLimited());
        if (Boolean.TRUE.equals(newData.getIsLimited())) {
            tt.setLimitQuantity(newData.getLimitQuantity());
        } else {
            tt.setLimitQuantity(null);
        }
        tt.setDescription(newData.getDescription());

        return repo.save(tt);
    }

    public void delete(Long id) {
        TicketType tt = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("票種不存在 ID=" + id));

        if (Boolean.FALSE.equals(tt.getIsDefault())) {
            throw new RuntimeException("系統模板不可刪除");
        }

        repo.delete(tt);
    }

    public List<TicketTypeDto> getAllDtos() {
        return getAll().stream()
                .map(tt -> new TicketTypeDto(
                        tt.getId(),
                        tt.getName(),
                        tt.getPrice(),
                        tt.getIsLimited(),
                        tt.getLimitQuantity(),
                        tt.getDescription()))
                .toList();
    }

    public List<TicketTypeDto> getTemplateDtos() {
        return getTemplates().stream()
                .map(tt -> new TicketTypeDto(
                        tt.getId(),
                        tt.getName(),
                        tt.getPrice(),
                        tt.getIsLimited(),
                        tt.getLimitQuantity(),
                        tt.getDescription()))
                .toList();
    }

    public List<TicketTypeDto> getCustomDtos(Long userId) {
        return getCustom(userId).stream()
                .map(tt -> new TicketTypeDto(
                        tt.getId(),
                        tt.getName(),
                        tt.getPrice(),
                        tt.getIsLimited(),
                        tt.getLimitQuantity(),
                        tt.getDescription()))
                .toList();
    }

    public TicketType applyTemplate(Long templateId, Long userId) {

        TicketType template = repo.findById(templateId)
                .orElseThrow(() -> new RuntimeException("找不到模板票種 ID=" + templateId));

        if (template.getIsDefault()) {
            throw new RuntimeException("只能套用系統模板（is_default = 0）");
        }

        // 建立新票種
        TicketType copy = new TicketType();
        copy.setName(template.getName());
        copy.setPrice(template.getPrice());
        copy.setIsLimited(template.getIsLimited());
        copy.setLimitQuantity(template.getLimitQuantity());
        copy.setDescription(template.getDescription());

        // 標記為主辦方自訂
        copy.setIsDefault(true);

        User u = new User();
        u.setId(userId);
        copy.setUser(u);

        return repo.save(copy);
    }

    public TicketTypeDto getOneDto(Long id) {
        TicketType tt = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("票種不存在 ID=" + id));

        // 這裡未做 userId 檢查，之後接登入再限制
        return new TicketTypeDto(
                tt.getId(),
                tt.getName(),
                tt.getPrice(),
                tt.getIsLimited(),
                tt.getLimitQuantity(),
                tt.getDescription());
    }

    public TicketTypeDto updateDto(Long id, TicketType newData) {
        TicketType tt = update(id, newData); // 你原本的更新方法
        return new TicketTypeDto(
                tt.getId(),
                tt.getName(),
                tt.getPrice(),
                tt.getIsLimited(),
                tt.getLimitQuantity(),
                tt.getDescription());
    }

}
