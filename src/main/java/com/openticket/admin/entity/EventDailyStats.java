package com.openticket.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "event_daily_stats")
@Getter
@Setter
public class EventDailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_shares")
    private Integer dayShares;

    @Column(name = "day_views")
    private Integer dayViews;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
}
