package com.openticket.admin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openticket.admin.dto.TicketTypeDto;
import com.openticket.admin.entity.TicketType;
import com.openticket.admin.repository.TicketTypeRepository;

@Service
public class TicketTypeService {

    @Autowired
    private TicketTypeRepository repo;

    public List<TicketType> getAll() {
        return repo.findAll();
    }

    public TicketType create(TicketType tt) {
        return repo.save(tt);
    }

    public TicketType update(Long id, TicketType newData) {
        TicketType tt = repo.findById(id).orElseThrow(() -> new RuntimeException("票種不存在 ID=" + id));

        tt.setName(newData.getName());
        tt.setPrice(newData.getPrice());
        tt.setIsLimited(newData.getIsLimited());
        tt.setLimitQuantity(newData.getLimitQuantity());

        return repo.save(tt);
    }

    public void delete(Long id) {
        repo.deleteById(id);
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
}
