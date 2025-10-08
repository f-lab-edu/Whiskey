package com.whiskey.domain.stock.enums;

import lombok.Getter;

@Getter
public enum StockStatus {
    SOLD_OUT("품절"),
    IN_STOCK("재고있음");

    private final String value;

    StockStatus(final String value) {
        this.value = value;
    }
}