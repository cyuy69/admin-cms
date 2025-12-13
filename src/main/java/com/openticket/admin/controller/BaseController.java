package com.openticket.admin.controller;

import org.springframework.ui.Model;

import com.openticket.admin.entity.Role;

import jakarta.servlet.http.HttpSession;

public abstract class BaseController {

    protected void setupRole(Model model, HttpSession session, Role role) {
        session.setAttribute("role", role);
        model.addAttribute("role", role);
        model.addAttribute("RoleEnum", Role.class);
    }
}
