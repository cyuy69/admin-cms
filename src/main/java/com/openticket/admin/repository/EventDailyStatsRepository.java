package com.openticket.admin.repository;

import com.openticket.admin.entity.EventDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventDailyStatsRepository extends JpaRepository<EventDailyStats, Long> {

    // 查某活動的某段日期
    List<EventDailyStats> findByEventIdAndStatDateBetween(
            Long eventId,
            LocalDate startDate,
            LocalDate endDate);

    // 今天瀏覽量總和（KPI 用）
    @Query("""
                select coalesce(sum(e.dayViews), 0)
                from EventDailyStats e
                where e.eventId = :eventId
                and e.statDate = :date
            """)
    int sumTodayViews(@Param("eventId") Long eventId,
            @Param("date") LocalDate date);
}
