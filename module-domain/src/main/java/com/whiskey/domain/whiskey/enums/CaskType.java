package com.whiskey.domain.whiskey.enums;

import lombok.Getter;

@Getter
public enum CaskType {
    SHERRY("SHERRY"),
    PORT("PORT"),
    BOURBON("BOURBON"),
    RUM("RUM"),
    MIZUNARA("MIZUNARA"),
    AMERICAN_OAK("AMERICAN_OAK"),
    FRENCH_OAK("FRENCH_OAK"),
    PEDRO_XIMENEZ("PEDRO_XIMENEZ"),
    OLOROSO("OLOROSO");

    private final String value;

    CaskType(String value) {
        this.value = value;
    }
}
