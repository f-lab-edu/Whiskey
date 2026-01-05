package com.whiskey.domain.whiskey.enums;

public enum WhiskeySortType {
    LATEST("latest", "최신순"),
    POPULAR("review_count", "리뷰 많은순");

    private final String value;
    private final String description;

    WhiskeySortType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}