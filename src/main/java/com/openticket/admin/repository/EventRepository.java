package com.openticket.admin.repository;

import com.openticket.admin.entity.Event;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCompanyUser_Id(Long userId);

    Optional<Event> findByIdAndCompanyUserId(Long id, Long userId);

    List<Event> findByCompanyUser_Id(Long companyId, Sort sort);

    @SuppressWarnings("null")
    @EntityGraph(attributePaths = { "images", "statusId" })
    List<Event> findAll();
}
