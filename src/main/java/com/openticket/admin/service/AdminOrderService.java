package com.openticket.admin.service;

import java.time.LocalDate;

import com.openticket.admin.dto.AdminOrderListDTO;
import com.openticket.admin.repository.AdminOrderRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;

@Service
public class AdminOrderService {

    private final AdminOrderRepository repo;

    public AdminOrderService(AdminOrderRepository repo) {
        this.repo = repo;
    }

    public Page<AdminOrderListDTO> searchOrders(
            String keyword,
            String startDateStr,
            String endDateStr,
            int page,
            int size) {

        LocalDateTime start = null;
        LocalDateTime end = null;

        boolean hasStart = startDateStr != null && !startDateStr.isEmpty();
        boolean hasEnd = endDateStr != null && !endDateStr.isEmpty();

        // 兩邊都沒填 → 查全部
        if (!hasStart && !hasEnd) {
        }

        // 單日（只填 start 或只填 end)
        if (hasStart && !hasEnd) {
            LocalDate day = LocalDate.parse(startDateStr);
            start = day.atStartOfDay();
            end = day.atTime(23, 59, 59);
        }

        if (hasEnd && !hasStart) {
            LocalDate day = LocalDate.parse(endDateStr);
            start = day.atStartOfDay();
            end = day.atTime(23, 59, 59);
        }

        // 區間查詢（兩邊都填）
        if (hasStart && hasEnd) {
            start = LocalDate.parse(startDateStr).atStartOfDay();
            end = LocalDate.parse(endDateStr).atTime(23, 59, 59);
        }

        Pageable pageable = PageRequest.of(page, size);

        return repo.searchOrders(keyword, start, end, pageable);
    }
}
