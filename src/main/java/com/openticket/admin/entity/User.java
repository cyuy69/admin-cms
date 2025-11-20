package com.openticket.admin.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;

    @Column(name = "password")
    @JsonIgnore
    private String passwd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToOne(mappedBy = "user")
    private CompanyProfile companyProfile;

    @OneToMany(mappedBy = "user")
    @JsonBackReference("announcement-user")
    private List<Announcement> announcements;

    @OneToMany(mappedBy = "companyUser")
    @JsonIgnore
    private List<Event> events;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<TicketType> ticketTypes;

}
