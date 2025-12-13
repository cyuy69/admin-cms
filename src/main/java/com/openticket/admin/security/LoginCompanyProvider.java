package com.openticket.admin.security;

import org.springframework.stereotype.Component;

import com.openticket.admin.entity.Role;

@Component
public class LoginCompanyProvider {

    /**
     * 暫時寫死
     * 之後改成 JWT / SecurityContext
     */
    public Long getCompanyId() {
        return 3L;
    }

    public Role getRole() {
        return Role.ADMIN; // or ADMIN
    }
}
