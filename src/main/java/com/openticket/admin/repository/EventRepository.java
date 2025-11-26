package com.openticket.admin.repository;

import com.openticket.admin.entity.Event;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCompanyUser_Id(Long userId);

    // 分頁查詢
    @EntityGraph(attributePaths = { "images" })
    Page<Event> findByCompanyUserId(Long companyId, Pageable pageable);

    // 搜尋 + 分頁
    @Query("SELECT e FROM Event e WHERE e.companyUser.id = :cid AND e.title LIKE :kw")
    @EntityGraph(attributePaths = { "images" })
    Page<Event> searchByCompanyUserId(
            @Param("cid") Long companyId,
            @Param("kw") String keyword,
            Pageable pageable);

    Optional<Event> findByIdAndCompanyUserId(Long id, Long userId);

    List<Event> findByCompanyUser_Id(Long companyId, Sort sort);

    @SuppressWarnings("null")
    @EntityGraph(attributePaths = { "images", "statusId" })
    List<Event> findAll();
}
