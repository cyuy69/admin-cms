package com.openticket.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.openticket.admin.entity.EventDetail;

public interface EventDetailRepository extends JpaRepository<EventDetail, Long> {
}
