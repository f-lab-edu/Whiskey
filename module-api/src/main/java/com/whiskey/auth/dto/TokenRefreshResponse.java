package com.whiskey.auth.dto;

import com.whiskey.domain.auth.dto.TokenInfo;

public record TokenRefreshResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiryTime
) {
    public static TokenRefreshResponse from(TokenInfo tokenInfo) {
        return new TokenRefreshResponse(
            tokenInfo.accessToken(),
            tokenInfo.refreshToken(),
            tokenInfo.tokenType(),
            tokenInfo.expiryTime()
        );
    }
}