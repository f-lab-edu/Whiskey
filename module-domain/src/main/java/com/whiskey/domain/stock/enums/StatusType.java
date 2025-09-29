package com.whiskey.domain.stock.enums;

import lombok.Getter;

@Getter
public enum StatusType {
    SOLD_OUT("품절"),
    IN_STOCK("재고있음");

    private final String value;

    StatusType(final String value) {
        this.value = value;
    }
}