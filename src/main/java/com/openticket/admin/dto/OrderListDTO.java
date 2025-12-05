package com.openticket.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderListDTO {
    private Long orderId;          // 訂單 ID（orders.id）
    private LocalDateTime createdAt; // 訂單建立時間（用 reservations.created_at）
    private String buyerName;      // 購買人（user.username）
    private String eventTitle;     // 活動名稱（event.title）
    private Integer ticketCount;   // 總張數（SUM(checkout_orders.quantity)）
    private BigDecimal totalAmount;// 總金額（reservations.totalAmount）
    private String status;         // 訂單狀態（orders.status）
}
