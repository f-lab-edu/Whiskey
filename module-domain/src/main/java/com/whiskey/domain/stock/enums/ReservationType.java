package com.whiskey.domain.stock.enums;

import lombok.Getter;

@Getter
public enum ReservationType {
    RESERVED("예약 중"),
    CONFIRMED("주문 확정"),
    CANCELLED("주문 취소"),
    EXPIRED("시간 초과");

    private final String value;

    ReservationType(final String value) {
        this.value = value;
    }
}
