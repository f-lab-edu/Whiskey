package com.whiskey.payment.exception;

import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {
    private final String code;

    public PaymentException(String code, String message) {
        super(message);
        this.code = code;
    }

    public PaymentException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
