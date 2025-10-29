package com.whiskey.domain.log.enums;

public enum ActivityType {
    VIEW("조회"),
    SEARCH("검색"),
    LOGIN("로그인");

    private final String activityName;

    ActivityType(final String activityName) {
        this.activityName = activityName;
    }
}
