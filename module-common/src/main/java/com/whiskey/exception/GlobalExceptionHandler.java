package com.whiskey.exception;

import com.whiskey.response.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleCommonException(Exception exception) {
        log.error(exception.getMessage(), exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        ApiResponse<Object> response = new ApiResponse<>(
            false,
            errorCode.name(),
            errorCode.getMessage(),
            null
        );

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException exception) {
        log.warn("code={}, message={}", exception.getErrorCode(), exception.getMessage());
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        log.warn("Validation 실패: {}", exception.getBindingResult().getFieldErrors());
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;

        // 필드별 에러 메시지 추가
        Map<String, String> errorMessage = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
            errorMessage.put(fieldError.getField(), fieldError.getDefaultMessage())
        );

        ApiResponse<Object> response = new ApiResponse<>(
            false,
            errorCode.name(),
            errorCode.getMessage(),
            errorMessage
        );

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
}