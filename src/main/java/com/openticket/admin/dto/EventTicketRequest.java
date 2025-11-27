package com.openticket.admin.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class EventTicketRequest {
    private Long ticketTemplateId;
    private BigDecimal customPrice;
    private Integer customLimit;
    private String description;
    private Boolean isEarlyBird;
    private Integer earlyBirdDays;
    private BigDecimal discountRate;
}
