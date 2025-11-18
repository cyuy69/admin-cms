package com.openticket.admin.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "event")
@Getter
@Setter
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    @JsonManagedReference
    private CompanyProfile company;

    private String title, address;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "event_start")
    private LocalDateTime eventStart;

    @Column(name = "event_end")
    private LocalDateTime eventEnd;

    @Column(name = "sale_start")
    private LocalDateTime ticketStart;

    @Column(name = "sale_end")
    private LocalDateTime ticketEnd;

    private Integer views;

    @Column(name = "tickets_sold")
    private Integer ticketsSold;

    private Integer shares;

    @Column(name = "avg_stay_time")
    private Integer avgStayTime;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private EventStatus statusId;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    @JsonManagedReference("event-image")
    private List<EventTitlePage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
