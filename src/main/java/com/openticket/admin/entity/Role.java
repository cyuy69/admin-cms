package com.openticket.admin.entity;

public enum Role {
    ADMIN(0),
    COMPANY(1),
    USER(2);

    public final int code;

    Role(int code) {
        this.code = code;
    }
}
