package com.whiskey.domain.payment.enums;

public enum PaymentStatus {
    PENDING("결제 대기"),
    COMPLETED("결제 완료"),
    FAILED("결제 실패"),
    EXPIRED("시간 초과"),
    CANCELLED("결제 취소");

    private final String value;

    PaymentStatus(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
