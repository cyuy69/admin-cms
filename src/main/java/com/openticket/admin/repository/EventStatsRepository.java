package com.openticket.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openticket.admin.entity.EventStats;

public interface EventStatsRepository extends JpaRepository<EventStats, Long> {

}
