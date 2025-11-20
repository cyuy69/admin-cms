package com.openticket.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.openticket.admin.service.AnnouncementService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
public class CompanyController {

    @Autowired
    private AnnouncementService announcementService;

    // =============廠商端=============
    // =============後台主頁=============
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long count = announcementService.count();
        model.addAttribute("content", "fragments/dashboard :: content");
        model.addAttribute("announcementCount", count);
        model.addAttribute("eventCount", 0);
        model.addAttribute("ticketCount", 0);
        return "index";
    }

    // =============後台分頁=============
    @GetMapping("/dashboard/**")
    public String dashboardSub(HttpServletRequest request, Model model) {
        String path = request.getRequestURI(); // e.g. /admin/dashboard/analytics/traffic
        String subPath = path.replace("/admin/dashboard/", ""); // e.g. analytics/traffic

        String fragmentPath = switch (subPath) {
            case "announcement" -> "fragments/announcement :: content";
            case "event", "event/ticket" -> "fragments/event :: content";

            case "analytics/traffic" -> "fragments/analytics/traffic :: content";
            case "analytics/consumer" -> "fragments/analytics/consumer :: content";
            case "analytics/summary" -> "fragments/analytics/summary :: content";

            default -> "fragments/dashboard :: content";
        };
        model.addAttribute("content", fragmentPath);
        return "index";
    }

    // =============公告與活動=============
    @GetMapping("/dashboard-frag")
    public String eventPage(Model model) {
        long count = announcementService.count();
        model.addAttribute("announcementCount", count);
        model.addAttribute("eventCount", 0);
        model.addAttribute("ticketCount", 0);
        return "fragments/dashboard :: content";
    }

    @GetMapping("/announcement-frag")
    public String annoFragment(Model model) {
        return "fragments/announcement :: content";
    }

    @GetMapping("/event-frag")
    public String eventFragment(Model model) {
        return "fragments/event :: content";
    }

    // =============數據分析=============
    @GetMapping("/analytics/traffic-frag")
    public String analyticsTraffic() {
        return "fragments/analytics/traffic :: content";
    }

    @GetMapping("/analytics/consumer-frag")
    public String analyticsConsumer() {
        return "fragments/analytics/consumer :: content";
    }

    @GetMapping("/analytics/summary-frag")
    public String analyticsSummary() {
        return "fragments/analytics/summary :: content";
    }

    // =============測試端=============
    @GetMapping("/test")
    public String test() {
        return "test";
    }

}
