package com.openticket.admin.controller.organizer;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openticket.admin.security.LoginCompanyProvider;
import com.openticket.admin.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private LoginCompanyProvider loginCompanyProvider;

    @GetMapping("/kpi")
    public Map<String, Object> getKpi() {

        Long companyId = loginCompanyProvider.getCompanyId();

        Map<String, Object> kpi = dashboardService.getOrganizerKpi(companyId);

        return Map.of(
                "success", true,
                "data", kpi);
    }
}
