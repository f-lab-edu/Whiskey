package com.whiskey.domain.stock.enums;

import lombok.Getter;

@Getter
public enum ReservationType {
    RESERVED("예약 증"),
    CONFIRMED("결제 확정"),
    CANCEL("주문 취소"),
    EXPIRED("시간 초과");

    private final String value;

    ReservationType(final String value) {
        this.value = value;
    }
}
