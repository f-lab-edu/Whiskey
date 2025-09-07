package com.whiskey.domain.user;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("ADMIN"),
    USER("USER");

    private final String role;

    Role(final String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}