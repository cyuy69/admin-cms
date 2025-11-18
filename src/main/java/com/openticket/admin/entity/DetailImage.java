package com.openticket.admin.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "detail_image")
@Getter
@Setter
public class DetailImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "caption")
    private String caption;

    @Column(name = "position")
    private Integer position;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "detail_id", nullable = false)
    @JsonBackReference("detail-image")
    private EventDetail eventDetail;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}