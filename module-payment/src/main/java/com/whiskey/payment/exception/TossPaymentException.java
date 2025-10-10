package com.whiskey.payment.exception;

import lombok.Getter;

@Getter
public class TossPaymentException extends RuntimeException {
    private final String code;

    public TossPaymentException(String code, String message) {
        super(message);
        this.code = code;
    }

    public TossPaymentException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
