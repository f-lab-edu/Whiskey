package com.whiskey.domain.whiskey.enums;

import lombok.Getter;

@Getter
public enum MaltType {
    SINGLE_MALT("SINGLE_MALT"),
    BLENDED("BLENDED");

    private final String value;

    MaltType(String value) {
        this.value = value;
    }
}
