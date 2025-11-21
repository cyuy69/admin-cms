package com.openticket.admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventDetail;

public interface EventDetailRepository extends JpaRepository<EventDetail, Long> {
    Optional<EventDetail> findByEvent(Event event);

    EventDetail findByEventId(Long eventId);

}
