package com.openticket.admin.repository;

import com.openticket.admin.entity.Announcement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnoRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByUserId(Long userId);
}
