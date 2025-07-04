package com.whiskey.exception;

import com.whiskey.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> commonException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();
        String message = exception.getMessage();
        Object data = exception.getData();

        ApiResponse<Object> response = new ApiResponse<>(
            false,
            errorCode.name(),
            message,
            data
        );

        return ResponseEntity.status(status).body(response);
    }
}