package com.openticket.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.openticket.admin.service.AnnouncementService;

@Controller
@RequestMapping("/admin")
public class DashboardController {

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
    @GetMapping("/dashboard/{sub}")
    public String dashboardSub(@PathVariable String sub, Model model) {
        String fragmentPath = switch (sub) {
            case "anno" -> "fragments/announcement :: content";
            case "event" -> "fragments/event :: content";
            default -> "fragments/dashboard :: content";
        };
        model.addAttribute("content", fragmentPath);
        return "index";
    }

    // =============活動=============
    @GetMapping("/dashboard-frag")
    public String eventPage(Model model) {
        model.addAttribute("announcementCount", 10);
        model.addAttribute("eventCount", 5);
        model.addAttribute("ticketCount", 100);
        return "fragments/dashboard :: content";
    }

    @GetMapping("/announcement-frag")
    public String annoFragment(Model model) {
        model.addAttribute("pageTitle", "公告管理");
        return "fragments/announcement :: content";
    }

    @GetMapping("/event-frag")
    public String eventFragment(Model model) {
        model.addAttribute("pageTitle", "活動管理");
        return "fragments/event :: content";
    }

    @GetMapping("/test")
    public String test() {
        return "test";
    }

}
