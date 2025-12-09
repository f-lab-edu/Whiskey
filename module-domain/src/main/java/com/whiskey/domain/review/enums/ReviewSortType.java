package com.whiskey.domain.review.enums;

public enum ReviewSortType {
    LATEST("latest", "최신순"),
    RATING_HIGH("rating_high","평점 높은순"),
    RATING_LOW("rating_low","평점 낮은순");

    private final String value;
    private final String description;

    ReviewSortType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public boolean isRatingSort() {
        return this == RATING_HIGH || this == RATING_LOW;
    }
}