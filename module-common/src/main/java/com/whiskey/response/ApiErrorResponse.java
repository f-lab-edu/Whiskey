package com.whiskey.response;

public record ApiErrorResponse<T>(
    boolean success,
    String code,
    String message,
    T data
) {
    public static <T> ApiErrorResponse<T> failure(String code, String message) {
        return new ApiErrorResponse(false, code, message, null);
    }

    public static <T> ApiErrorResponse<T> failure(String code, String message, T data) {
        return new ApiErrorResponse(false, code, message, data);
    }
}
