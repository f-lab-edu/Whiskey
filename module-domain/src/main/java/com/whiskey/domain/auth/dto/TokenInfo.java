package com.whiskey.domain.auth.dto;

public record TokenInfo(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiryTime
) {}