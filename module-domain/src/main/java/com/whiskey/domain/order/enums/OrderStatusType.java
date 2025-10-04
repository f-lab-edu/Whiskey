package com.whiskey.domain.order.enums;

import lombok.Getter;

@Getter
public enum OrderStatusType {
    PENDING("주문 생성"),
    CONFIRMED("주문 확정"),
    CANCELLED("주문 취소"),
    EXPIRED("시간 초과");

    private final String value;

    OrderStatusType(final String value) {
        this.value = value;
    }
}
