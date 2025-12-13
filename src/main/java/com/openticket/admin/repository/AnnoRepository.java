package com.openticket.admin.repository;

import com.openticket.admin.entity.Announcement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface AnnoRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByUserId(Long userId);

    @Query("SELECT a FROM Announcement a WHERE a.user.role = 0")
    List<Announcement> findAdminAnnouncements();

}
