package com.openticket.admin.service;

import com.openticket.admin.entity.EventStats;
import com.openticket.admin.repository.EventStatsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventStatsService {

    private final EventStatsRepository eventStatsRepository;

    public EventStatsService(EventStatsRepository eventStatsRepository) {
        this.eventStatsRepository = eventStatsRepository;
    }

    /** 查詢單筆活動統計 */
    public EventStats getStats(Long eventId) {
        return eventStatsRepository.findById(eventId).orElse(null);
    }

    /** 查詢多個活動統計 */
    public List<EventStats> getStatsForEvents(List<Long> eventIds) {
        return eventStatsRepository.findByIdIn(eventIds);
    }
}
