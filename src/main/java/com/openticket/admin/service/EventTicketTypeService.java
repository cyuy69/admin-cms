package com.openticket.admin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openticket.admin.dto.EventTicketRequest;
import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventTicketType;
import com.openticket.admin.entity.TicketType;
import com.openticket.admin.repository.EventTicketTypeRepository;
import com.openticket.admin.repository.TicketTypeRepository;

@Service
public class EventTicketTypeService {

    @Autowired
    private EventTicketTypeRepository repo;

    @Autowired
    private TicketTypeRepository ticketTypeRepo;

    public void createForEvent(Event event, List<EventTicketRequest> ticketList) {

        for (EventTicketRequest req : ticketList) {

            TicketType template = ticketTypeRepo.findById(req.getTicketTemplateId())
                    .orElseThrow(() -> new RuntimeException("找不到模板票種 ID：" + req.getTicketTemplateId()));

            EventTicketType ett = new EventTicketType();
            ett.setEvent(event);
            ett.setTicketTemplate(template);

            // 活動票價：活動覆蓋 > 模板 > null
            ett.setCustomPrice(
                    req.getCustomPrice() != null
                            ? req.getCustomPrice()
                            : template.getPrice());

            // 活動限量：活動覆蓋 > 模板 > null
            Integer finalLimit = req.getCustomLimit() != null
                    ? req.getCustomLimit()
                    : template.getLimitQuantity();

            ett.setCustomLimit(finalLimit);

            // isLimited 自動判斷（非常重要！）
            ett.setIsLimited(finalLimit != null);

            repo.save(ett);
        }
    }
}
