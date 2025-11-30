package com.openticket.admin.controller;

import com.openticket.admin.dto.AnalyticsDTO;
import com.openticket.admin.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsApiController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping
    public AnalyticsDTO getAnalytics(
            @RequestParam List<Long> eventIds,
            @RequestParam(defaultValue = "7") int period,
            @RequestParam(defaultValue = "merge") String mode) {
        return analyticsService.getAnalytics(eventIds, period, mode);
    }

}