package com.whiskey.domain.member.enums;

public enum MemberRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    MemberRole(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}
