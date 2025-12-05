package com.openticket.admin.service;

import com.openticket.admin.dto.EventTitleDTO;
import com.openticket.admin.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final EventRepository eventRepository;

    public List<EventTitleDTO> getEventTitles(Long organizerId, String keyword) {

        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        List<Object[]> rows = eventRepository.findMyEventTitles(organizerId, keyword);

        List<EventTitleDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            EventTitleDTO dto = new EventTitleDTO();
            dto.setId(((Number) row[0]).longValue());
            dto.setTitle((String) row[1]);
            result.add(dto);
        }

        return result;
    }
}