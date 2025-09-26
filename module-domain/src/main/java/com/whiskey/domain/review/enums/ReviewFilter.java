package com.whiskey.domain.review.enums;

public enum ReviewFilter {
    ACTIVE("활성"),
    DELETED("삭제된"),
    ALL("전체");

    private final String description;

    ReviewFilter(String description) {
        this.description = description;
    }
}
