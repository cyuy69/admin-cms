package com.openticket.admin.service.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openticket.admin.dto.EventListItemDTO;
import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventDetail;
import com.openticket.admin.entity.EventStats;
import com.openticket.admin.repository.EventDetailRepository;
import com.openticket.admin.repository.EventRepository;
import com.openticket.admin.repository.EventStatsRepository;

@Service
public class EventService {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventStatsRepository eventStatsRepository;

    @Autowired
    private EventDetailRepository detailRepo;

    public Event findById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到活動 ID=" + id));
    }

    @Transactional
    public void save(Event event) {
        eventRepository.save(event);
    }

    @Transactional
    public void updateDetail(Event event, String content) {

        // 找 event_detail
        EventDetail detail = detailRepo.findByEvent(event).orElse(null);

        if (content == null || content.isBlank()) {
            // 若無內容 → 不建立詳細內容
            return;
        }

        if (detail == null) {
            // 新增
            detail = new EventDetail();
            detail.setEvent(event);
        }

        detail.setContent(content);
        detailRepo.save(detail);
    }

    public List<Event> getAllEvents(Long companyId) {
        return eventRepository.findByCompanyUser_Id(companyId);
    }

    public Event getEventById(Long id, Long companyId) {
        return eventRepository.findByIdAndCompanyUserId(id, companyId)
                .orElseThrow(() -> new RuntimeException("活動不存在或沒有權限"));
    }

    @Transactional
    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(Event updated) {
        return eventRepository.save(updated);
    }

    public List<Map<String, Object>> getAllEventsWithStats() {
        List<Event> events = eventRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Event e : events) {
            Map<String, Object> map = new HashMap<>();
            // ... 填入 event 欄位

            // 查詢對應的統計資料（可能為 null）
            EventStats stats = eventStatsRepository.findById(e.getId()).orElse(null);

            map.put("views", stats != null ? stats.getViews() : 0);
            map.put("shares", stats != null ? stats.getShares() : 0);
            map.put("ticketsSold", getTicketsSold(e.getId())); // 動態計算

            result.add(map);
        }
        return result;
    }

    private int getTicketsSold(Long id) {
        return (int) ((id * 13 % 1500) + 1);
    }

    public Page<EventListItemDTO> getEventListPage(Long companyId, String keyword, Pageable pageable) {

        Page<Event> events;

        // 搜尋 or 全部
        if (keyword == null || keyword.isBlank()) {
            events = eventRepository.findByCompanyUserId(companyId, pageable);
        } else {
            events = eventRepository.searchByCompanyUserId(companyId, "%" + keyword + "%", pageable);
        }

        // 轉 DTO
        return events.map(ev -> {
            EventListItemDTO item = new EventListItemDTO();
            item.setId(ev.getId());
            item.setTitle(ev.getTitle());
            item.setEventStart(ev.getEventStartFormatted());
            item.setEventEnd(ev.getEventEndFormatted());
            item.setTicketStart(ev.getTicketStartFormatted());
            item.setCreatedAt(ev.getCreatedAtIso());
            item.setStatus(calculateDynamicStatus(ev));

            // 假資料
            item.setViews(0);
            item.setTicketsSold(getTicketsSold(ev.getId()));

            return item;
        });
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    public String calculateDynamicStatus(Event event) {
        if (event.getStatus() != null && "已取消".equals(event.getStatus().getStatus())) {
            return "已取消";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sale = event.getTicketStart();
        LocalDateTime start = event.getEventStart();
        LocalDateTime end = event.getEventEnd();

        if (sale == null || start == null || end == null) {
            return event.getStatus() != null ? event.getStatus().getStatus() : "未設定";
        }

        if (now.isBefore(sale))
            return "未開放";
        if (now.isBefore(start))
            return "開放購票";
        if (now.isBefore(end))
            return "活動進行中";
        return "已結束";
    }

    public Event findOwnedEvent(Long eventId, Long companyId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("找不到活動 ID=" + eventId));

        if (!event.getCompanyUser().getId().equals(companyId)) {
            throw new RuntimeException("無權限存取此活動");
        }

        return event;
    }

}
