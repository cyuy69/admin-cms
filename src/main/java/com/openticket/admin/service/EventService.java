package com.openticket.admin.service;

import com.openticket.admin.dto.EventListItemDTO;
import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventDetail;
import com.openticket.admin.entity.EventStats;
import com.openticket.admin.repository.EventDetailRepository;
import com.openticket.admin.repository.EventRepository;
import com.openticket.admin.repository.EventStatsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public void save(Event event) {
        eventRepository.save(event);
    }

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

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event createEvent(Event event) {
        return eventRepository.save(event);
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

    public List<EventListItemDTO> getEventListItems() {
        long start = System.currentTimeMillis();

        List<Event> events = eventRepository.findAll();
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, EventStats> statsMap = eventStatsRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(EventStats::getId, stats -> stats));
        List<EventListItemDTO> items = new ArrayList<>();

        for (Event e : events) {
            EventListItemDTO item = new EventListItemDTO();
            item.setId(e.getId());
            item.setTitle(e.getTitle());
            item.setImages(e.getImages());

            item.setEventStart(e.getEventStartFormatted());
            item.setEventEnd(e.getEventEndFormatted());
            item.setTicketStart(e.getTicketStartFormatted());
            item.setCreatedAt(e.getCreatedAt().toString());

            item.setStatus(e.getDynamicStatus()); // "開放購票" / "活動進行中" ...

            // 統計資料（目前用假資料）
            item.setViews(0);
            EventStats stats = eventStatsRepository.findById(e.getId()).orElse(null);
            item.setViews(stats != null ? stats.getViews() : 0);

            item.setTicketsSold(getTicketsSold(e.getId())); // 你的假方法

            items.add(item);
        }
        long end = System.currentTimeMillis();
        System.out.println("載入活動列表耗時：" + (end - start) + "ms");

        return items;
    }

}
