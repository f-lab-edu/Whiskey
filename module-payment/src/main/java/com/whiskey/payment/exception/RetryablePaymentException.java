package com.whiskey.payment.exception;

public class RetryablePaymentException extends RuntimeException {

    public RetryablePaymentException(String message) {
        super(message);
    }

    public RetryablePaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
