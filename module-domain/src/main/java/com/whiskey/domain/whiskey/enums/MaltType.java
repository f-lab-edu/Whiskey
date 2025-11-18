package com.whiskey.domain.whiskey.enums;

import lombok.Getter;

@Getter
public enum MaltType {
    SINGLE_MALT("SINGLE_MALT"),
    BLENDED("BLENDED"),
    TENNESSEE_WHISKEY("TENNESSEE_WHISKEY"),
    BOURBON("BOURBON"),
    RYE("RYE"),
    PURE_POT_STILL("PURE_POT_STILL"),
    CANADIAN("CANADIAN");

    private final String value;

    MaltType(String value) {
        this.value = value;
    }
}
