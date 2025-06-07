package com.whiskey.auth.controller;

import com.whiskey.auth.dto.LoginRequest;
import com.whiskey.auth.service.AuthService;
import com.whiskey.domain.login.AccessTokenHeader;
import com.whiskey.domain.login.LoginToken;
import com.whiskey.domain.login.RefreshTokenCookie;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginToken loginToken = authService.login(loginRequest.email(), loginRequest.password());
        RefreshTokenCookie refreshTokenCookie = loginToken.refreshTokenCookie();

        ResponseCookie responseCookie = ResponseCookie.from(refreshTokenCookie.refreshToken(), refreshTokenCookie.value())
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(refreshTokenCookie.tokenDuration())
            .build();

        AccessTokenHeader accessTokenHeader = loginToken.accessTokenHeader();

        return ResponseEntity.noContent()
            .header(accessTokenHeader.name(), accessTokenHeader.value())
            .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
            .build();
    }
}
