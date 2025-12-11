package com.openticket.admin.controller.admin;

import com.openticket.admin.dto.AdminOrderListDTO;
import com.openticket.admin.service.AdminOrderService;

import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderApiController {

    private final AdminOrderService service;

    public AdminOrderApiController(AdminOrderService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AdminOrderListDTO> listAdminOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return service.searchOrders(keyword, startDate, endDate, page, size);
    }
}
