package com.whiskey.exception;

import org.springframework.http.HttpStatus;

public class CommonException extends RuntimeException {
    private final CommonErrorCode errorCode;
    private final Object data;

    public CommonException(CommonErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }

    public CommonException(CommonErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public CommonErrorCode errorCode() {
        return errorCode;
    }

    public Object data() {
        return data;
    }

    public HttpStatus httpStatus() {
        return errorCode.getHttpStatus();
    }
}
