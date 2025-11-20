package com.openticket.admin.dto;

import java.math.BigDecimal;

public record TicketTypeDto(
        Long id,
        String name,
        BigDecimal price,
        boolean isLimited,
        Integer limitQuantity,
        String description) {
}
