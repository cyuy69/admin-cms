package com.openticket.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventStats {

    @Id
    @Column(name = "event_id")
    private Long id;
    private Integer views;
    private Integer shares;

}
