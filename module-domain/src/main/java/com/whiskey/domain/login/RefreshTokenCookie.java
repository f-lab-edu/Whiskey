package com.whiskey.domain.login;

import java.time.Duration;

public record RefreshTokenCookie(String refreshToken, String value, Duration tokenDuration) {

}
