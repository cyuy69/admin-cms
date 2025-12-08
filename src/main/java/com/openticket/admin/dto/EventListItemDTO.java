package com.openticket.admin.dto;

import java.util.List;

import com.openticket.admin.entity.EventTitlePage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventListItemDTO {
    private Long id;
    private String title; // 活動名稱
    private String eventStart; // 開始時間（已格式化）
    private String eventEnd; // 結束時間（已格式化）
    private String ticketStart; // 售票日期（已格式化）
    private String status; // 狀態

    private String createdAt;
    private List<EventTitlePage> images;

    private Integer views; // 瀏覽
    private Integer ticketsSold;
    private Integer shares; // 分享數
    private Long revenue; // 總收入
}
