package com.openticket.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.openticket.admin.entity.Role;
import com.openticket.admin.security.LoginCompanyProvider;

import jakarta.servlet.http.HttpSession;

@Controller
public class AnnouncementController extends BaseController {

    @Autowired
    private LoginCompanyProvider loginProvider;

    @GetMapping({ "/admin/announcement", "/organizer/announcement" })
    public String announcement(Model model, HttpSession session) {
        Role role = loginProvider.getRole();
        setupRole(model, session, role);

        // 共用 fragment
        model.addAttribute("content", "fragments/announcement :: content");
        return "index";
    }

    // fragment 用
    @GetMapping({ "/admin/announcement-frag", "/organizer/announcement-frag" })
    public String announcementFrag() {
        return "fragments/announcement :: content";
    }
}
