package com.openticket.admin.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.openticket.admin.entity.Announcement;

public interface AnnoRepository extends JpaRepository<Announcement, Long> {
    Page<Announcement> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE a.user.role = 0")
    Page<Announcement> findAdminAnnouncements(Pageable pageable);

    Page<Announcement> findByTitleContainingOrContentContaining(String title, String content, Pageable pageable);

}
