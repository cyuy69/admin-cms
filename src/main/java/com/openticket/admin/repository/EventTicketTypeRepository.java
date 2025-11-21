package com.openticket.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventTicketType;

public interface EventTicketTypeRepository extends JpaRepository<EventTicketType, Long> {
    List<EventTicketType> findByEventId(Long eventId);

    void deleteByEvent(Event event);
}
