package com.whiskey.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum WhiskeyErrorCode implements BaseErrorCode {
    WHISKEY_NOT_FOUND(HttpStatus.NOT_FOUND, "위스키를 찾을 수 없습니다."),
    WHISKEY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 위스키입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    WhiskeyErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
