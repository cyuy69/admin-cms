package com.openticket.admin.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.openticket.admin.entity.Role;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("role")
    public Role addRoleToModel(HttpSession session) {
        Object role = session.getAttribute("role");
        return role != null ? (Role) role : null;
    }
}
