package com.whiskey.exception;

import org.springframework.http.HttpStatus;

public interface GlobalErrorCode {
    HttpStatus getHttpStatus();
    String getMessage();
    String name();
}