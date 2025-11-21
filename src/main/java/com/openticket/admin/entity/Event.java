package com.openticket.admin.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.openticket.admin.utils.DateTimeUtil;

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
import jakarta.persistence.Transient;
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
    @JsonIgnore
    private User companyUser;

    private String title, address;

    @Column(name = "event_start")
    private LocalDateTime eventStart;

    @Column(name = "event_end")
    private LocalDateTime eventEnd;

    @Column(name = "sale_start")
    private LocalDateTime ticketStart;

    @Column(name = "avg_stay_time")
    private Integer avgStayTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private EventStatus statusId;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    @JsonManagedReference("event-image")
    private List<EventTitlePage> images = new ArrayList<>();

    @Transient
    public String getDynamicStatus() {
        // 1. 已取消優先
        if (this.statusId != null && "已取消".equals(this.statusId.getStatus())) {
            return "已取消";
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. 先把時間欄位抓出來，避免一直呼叫 getter
        LocalDateTime sale = this.ticketStart;
        LocalDateTime start = this.eventStart;
        LocalDateTime end = this.eventEnd;

        // 3. 如果舊資料沒有設定時間，避免 NPE，直接用資料庫狀態 or 給預設字串
        if (sale == null || start == null || end == null) {
            // 如果你希望顯示資料庫裡的中文狀態可以這樣：
            if (this.statusId != null && this.statusId.getStatus() != null) {
                return this.statusId.getStatus(); // ex. "未開放"、"活動進行中"...
            }
            // 或者直接顯示「未設定」
            return "未設定";
        }

        // 4. 以下才是正常的時間判斷
        if (now.isBefore(sale)) {
            return "未開放";
        }
        if (now.isBefore(start)) {
            return "開放購票";
        }
        if (now.isBefore(end)) {
            return "活動進行中";
        }
        return "已結束";
    }

    @Transient
    public String getEventStartFormatted() {
        return DateTimeUtil.format(this.eventStart);
    }

    @Transient
    public String getEventEndFormatted() {
        return DateTimeUtil.format(this.eventEnd);
    }

    @Transient
    public String getTicketStartFormatted() {
        return DateTimeUtil.format(this.ticketStart);
    }

    @Transient
    public String getCreatedAtIso() {
        return createdAt != null ? createdAt.toString() : null;
    }

}
