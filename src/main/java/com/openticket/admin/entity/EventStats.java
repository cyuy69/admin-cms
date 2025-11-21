package com.openticket.admin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "event_stats")
@Getter
@Setter
public class EventStats {

    @Id
    @Column(name = "event_id")
    private Long id;
    private Integer views;
    private Integer shares;

    private LocalDateTime updatedAt;

}
