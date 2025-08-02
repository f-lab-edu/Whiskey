package com.whiskey.domain.whiskey.enums;

import lombok.Getter;

@Getter
public enum CaskType {
    SHERRY("SHERRY"),
    PORT("PORT"),
    BOURBON("BOURBON"),
    RUM("RUM"),
    MIZUNARA("MIZUNARA"),;

    private final String value;

    CaskType(String value) {
        this.value = value;
    }
}
