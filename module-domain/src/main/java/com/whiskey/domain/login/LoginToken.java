package com.whiskey.domain.login;

public record LoginToken(AccessTokenHeader accessTokenHeader, RefreshTokenCookie refreshTokenCookie) {

}
