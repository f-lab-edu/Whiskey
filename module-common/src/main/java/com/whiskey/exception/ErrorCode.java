package com.whiskey.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Deprecated
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "유효성 검증에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다. 잠시 후 다시 시도해주세요."),
    CONFLICT(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스가 존재하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public BusinessException exception(String message) {
        return new BusinessException(this, message);
    }

    public BusinessException exception(String message, Object data) {
        return new BusinessException(this, message, data);
    }
}