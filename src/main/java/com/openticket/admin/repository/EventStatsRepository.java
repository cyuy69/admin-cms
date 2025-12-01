package com.openticket.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openticket.admin.entity.EventStats;

public interface EventStatsRepository extends JpaRepository<EventStats, Long> {
    List<EventStats> findByIdIn(List<Long> eventIds);
}
