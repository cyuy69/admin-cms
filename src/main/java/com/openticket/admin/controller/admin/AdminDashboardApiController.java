package com.openticket.admin.controller.admin;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openticket.admin.dto.AdminAnalyticsDTO;
import com.openticket.admin.entity.LoginLog;
import com.openticket.admin.service.AnalyticsService;
import com.openticket.admin.service.LoginLogService;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardApiController {

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/login-logs")
    public Page<LoginLog> getLoginLogs(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return loginLogService.searchLoginLogs(keyword, pageable);
    }

    @GetMapping("/dashboard-analytics")
    public AdminAnalyticsDTO getAdminAnalytics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate s = (startDate == null || startDate.isEmpty())
                ? null
                : LocalDate.parse(startDate);

        LocalDate e = (endDate == null || endDate.isEmpty())
                ? null
                : LocalDate.parse(endDate);

        return analyticsService.getAdminAnalytics(s, e);
    }

}
