package com.whiskey.auth.service;

import com.whiskey.domain.login.AccessTokenHeader;
import com.whiskey.domain.login.LoginToken;
import com.whiskey.domain.login.RefreshTokenCookie;
import com.whiskey.domain.member.enums.MemberRole;
import com.whiskey.security.jwt.JwtProperties;
import com.whiskey.security.jwt.JwtProvider;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final JwtProperties jwtProperties;
    private final JwtProvider jwtProvider;

    public static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    public static final String AUTH_HEADER_NAME = "Authorization";
    public static final String AUTH_HEADER_PREFIX = "Bearer ";

    public LoginToken login(Long id, MemberRole memberRole) {
        String refreshToken = storeRefreshToken(id, memberRole);

        AccessTokenHeader accessTokenHeader = new AccessTokenHeader(AUTH_HEADER_NAME, AUTH_HEADER_PREFIX + jwtProvider.generateToken(id, memberRole.getAuthority()));
        RefreshTokenCookie refreshTokenCookie = new RefreshTokenCookie(REFRESH_COOKIE_NAME, refreshToken, Duration.ofMillis(jwtProperties.getRefreshTokenValidity()));
        return new LoginToken(accessTokenHeader, refreshTokenCookie);
    }

    public String storeRefreshToken(Long id, MemberRole memberRole) {
        String refreshToken = jwtProvider.generateRefreshToken(id, memberRole.getAuthority());
        return refreshToken;
    }
}