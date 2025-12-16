package com.openticket.admin.security;

import org.springframework.stereotype.Component;

import com.openticket.admin.entity.Role;

@Component
public class LoginCompanyProvider {

    // public Long getCompanyId() {
    // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    // if (auth != null && auth.getPrincipal() instanceof AccountPrincipal
    // principal) {
    // return principal.getCompanyId();
    // }
    // return null;
    // }

    // public Role getRole() {
    // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    // if (auth != null && auth.getPrincipal() instanceof AccountPrincipal
    // principal) {
    // return principal.getRoleEnum();
    // }
    // return null;
    // }
    // }
    /**
     * 暫時寫死
     * 後端不一致的跨域問題
     * 因此jwt、cookie、session方法都用不了
     */
    public Long getCompanyId() {
        return 2L;
    }

    public Role getRole() {
        return Role.COMPANY; // COMPANY or ADMIN
    }
}
