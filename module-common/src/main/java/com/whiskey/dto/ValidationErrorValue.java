package com.whiskey.dto;

public record ValidationErrorValue(
    String field,
    Object value,
    String code
) {}
