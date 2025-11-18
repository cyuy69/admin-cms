package com.openticket.admin.repository;

import com.openticket.admin.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStatusRepository extends JpaRepository<EventStatus, Long> {
}