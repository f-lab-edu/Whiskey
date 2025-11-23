package com.whiskey.payment.enums;

public enum TossPaymentStatus {
    READY,
    IN_PROGRESS,
    WAITING_FOR_DEPOSIT,
    DONE,
    CANCELED,
    PARTIAL_CANCELED,
    ABORTED,
    EXPIRED;

    public static TossPaymentStatus from(String status) {
        try {
            return TossPaymentStatus.valueOf(status);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 결제 처리상태입니다. Toss에 문의하세요.: " + status);
        }
    }

    public boolean isCanceled() {
        return this == CANCELED;
    }

    public boolean isApproved() {
        return this == DONE;
    }
}