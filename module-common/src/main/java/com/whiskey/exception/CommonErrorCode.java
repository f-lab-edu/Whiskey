package com.whiskey.exception;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "유효성 검증에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다. 잠시 후 다시 시도해주세요."),
    CONFLICT(HttpStatus.CONFLICT, "이미 처리된 요청입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    CommonErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public CommonException exception() {
        return new CommonException(this, this.message);
    }

    public CommonException exception(String message) {
        return new CommonException(this, message);
    }

    public CommonException exception(String message, Object data) {
        return new CommonException(this, message, data);
    }
}
