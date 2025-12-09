package com.openticket.admin.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "event_ticket_type")
@Getter
@Setter
public class EventTicketType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_limited", nullable = false)
    private Boolean isLimited;

    @Column(name = "is_early_bird")
    private Boolean isEarlyBird;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "custom_limit")
    private Integer customLimit;

    @Column(name = "custom_price")
    private BigDecimal customPrice;

    // 所屬活動
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonBackReference("event-ticket")
    private Event event;

    // 使用哪個主辦方的模板
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_template_id", nullable = false)
    private TicketType ticketTemplate;

    // 早鳥票相關
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "early_bird_config_id")
    private TicketDiscountConfig earlyBirdConfig;

    // 票種
    @OneToMany(mappedBy = "eventTicketType")
    private List<CheckoutOrder> checkoutOrders;

    // 自動填入時間
    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
