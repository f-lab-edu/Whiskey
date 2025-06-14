package com.whiskey.exception;

import static org.springframework.http.ResponseEntity.*;

import com.whiskey.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ApiResponse> commonException(CommonException exception) {
        HttpStatus status = exception.getErrorCode().getHttpStatus();
        String responseCode = exception.getErrorCode().name();

        ApiResponse response = ApiResponse.failure(responseCode, exception.getMessage(), exception.getData());

        return status(status).body(response);
    }
}