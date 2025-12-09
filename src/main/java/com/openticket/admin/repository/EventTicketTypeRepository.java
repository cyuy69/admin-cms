package com.openticket.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventTicketType;

public interface EventTicketTypeRepository extends JpaRepository<EventTicketType, Long> {
    List<EventTicketType> findByEventId(Long eventId);

    void deleteByEvent(Event event);

    @Query("SELECT COUNT(r) > 0 FROM ReservationItem r WHERE r.eventTicketType.id = :ettId")
    boolean hasOrders(@Param("ettId") Long eventTicketTypeId);
}
