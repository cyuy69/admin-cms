package com.openticket.admin.repository;

import com.openticket.admin.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnoRepository extends JpaRepository<Announcement, Long> {
}
