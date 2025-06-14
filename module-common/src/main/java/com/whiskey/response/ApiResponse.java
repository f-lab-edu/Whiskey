package com.whiskey.response;

import com.whiskey.response.enums.SuccessCode;

public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data) {
        public static <T> ApiResponse<T> success(SuccessCode successCode) {
            return new ApiResponse<>(true, successCode.getCode(), successCode.getMessage(), null);
        }

        public static <T> ApiResponse<T> success(SuccessCode successCode, T data) {
            return new ApiResponse<>(true, successCode.getCode(), successCode.getMessage(), data);
        }

        public static <T> ApiResponse<T> failure(String code, String message) {
            return new ApiResponse<>(false, code, message, null);
        }

        public static <T> ApiResponse<T> failure(String code, String message, T data) {
            return new ApiResponse<>(false, code, message, data);
        }
}
