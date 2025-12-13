package com.openticket.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.openticket.admin.entity.Role;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController extends BaseController {
    // 出口管理(如果有權限或session可以直接靠網址進)
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        setupRole(model, session, Role.ADMIN);
        model.addAttribute("content", "fragments/admin/admin-dashboard :: content");
        return "index";
    }

    @GetMapping("/dashboard/users")
    public String users(Model model, HttpSession session) {
        setupRole(model, session, Role.ADMIN);
        model.addAttribute("content", "fragments/admin/users :: content");
        return "index";
    }

    @GetMapping("/dashboard/orders")
    public String orders(Model model, HttpSession session) {
        setupRole(model, session, Role.ADMIN);
        model.addAttribute("content", "fragments/admin/orders :: content");
        return "index";
    }

    // 後台首頁分頁
    @GetMapping("/dashboard-frag")
    public String dashboardFrag(Model model) {
        return "fragments/admin/admin-dashboard :: content";
    }

    // 使用者權限管理分頁
    @GetMapping("/dashboard/users-frag")
    public String usersFrag(Model model) {
        return "fragments/admin/users :: content";
    }

    // 訂單管理分頁
    @GetMapping("/dashboard/orders-frag")
    public String adminOrdersFrag(Model model) {
        return "fragments/admin/orders :: content";
    }

}
