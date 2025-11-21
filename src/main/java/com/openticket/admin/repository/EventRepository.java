package com.openticket.admin.repository;

import com.openticket.admin.entity.Event;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(attributePaths = { "images", "statusId" })
    List<Event> findAll();
}
