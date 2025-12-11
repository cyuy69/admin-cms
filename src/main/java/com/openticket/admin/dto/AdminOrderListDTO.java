package com.openticket.admin.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AdminOrderListDTO {

    private Long orderId;
    private LocalDateTime createdAt;
    private String buyerName;
    private String buyerAccount;
    private String eventTitle;
    private Integer totalQuantity;
    private String status;

    // 客製化的建構式，主要是totalQuantity不能靠@AllArgsConstructor寫，因為有計算邏輯
    public AdminOrderListDTO(
            Long orderId,
            LocalDateTime createdAt,
            String buyerName,
            String buyerAccount,
            String eventTitle,
            Long totalQuantity,
            String status) {
        this.orderId = orderId;
        this.createdAt = createdAt;
        this.buyerName = buyerName;
        this.buyerAccount = buyerAccount;
        this.eventTitle = eventTitle;
        this.totalQuantity = totalQuantity != null ? totalQuantity.intValue() : 0;
        this.status = status;
    }
}
