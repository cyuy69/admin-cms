package com.openticket.admin.service;

import com.openticket.admin.dto.OrderListDTO;
import com.openticket.admin.repository.OrderRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderListDTO> getOrdersByEvent(Long eventId, String keyword) {
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        List<Object[]> rows = orderRepository.findOrdersByEventId(eventId, keyword);
        List<OrderListDTO> list = new ArrayList<>();

        for (Object[] row : rows) {
            OrderListDTO dto = new OrderListDTO();

            // 0: orderId (Long)
            dto.setOrderId(((Number) row[0]).longValue());

            // 1: createdAt (Timestamp → LocalDateTime)
            Timestamp ts = (Timestamp) row[1];
            LocalDateTime createdAt = ts.toLocalDateTime();
            dto.setCreatedAt(createdAt);

            // 2: buyerName
            dto.setBuyerName((String) row[2]);

            // 3: eventTitle
            dto.setEventTitle((String) row[3]);

            // 4: ticketCount (Long/BigInteger → int)
            dto.setTicketCount(((Number) row[4]).intValue());

            // 5: totalAmount (BigDecimal)
            dto.setTotalAmount((BigDecimal) row[5]);

            // 6: status
            dto.setStatus((String) row[6]);

            list.add(dto);
        }

        return list;
    }
}
