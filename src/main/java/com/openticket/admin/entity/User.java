package com.openticket.admin.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    @Column(nullable = false)
    private String account;

    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String passwd;

    private String username;

    private String tel;

    private String address;

    @Column(name = "role")
    private Integer role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "user")
    @JsonBackReference("announcement-user")
    private List<Announcement> announcements;

    @OneToMany(mappedBy = "companyUser")
    @JsonIgnore
    private List<Event> events;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<TicketType> ticketTypes;

    @JsonIgnore
    public Role getRoleEnum() {
        return this.role != null ? Role.fromCode(this.role) : null;
    }

    public void setRoleEnum(Role roleEnum) {
        this.role = roleEnum != null ? roleEnum.getCode() : null;
    }

}
