package com.whiskey.auth.controller;

import com.whiskey.auth.dto.LoginRequest;
import com.whiskey.auth.dto.TokenRefreshRequest;
import com.whiskey.auth.dto.TokenRefreshResponse;
import com.whiskey.auth.service.AuthService;
import com.whiskey.domain.auth.JwtResponse;
import com.whiskey.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.login(loginRequest);
        return ApiResponse.success("로그인에 성공하였습니다.", response);
    }

    @PostMapping("/token/refresh")
    public ApiResponse<TokenRefreshResponse> refreshTokenRequest(@Valid @RequestBody TokenRefreshRequest refreshTokenRequest) {
        TokenRefreshResponse response = authService.refreshAccessToken(refreshTokenRequest.refreshToken());
        return ApiResponse.success("Refresh Token 갱신", response);
    }
}
