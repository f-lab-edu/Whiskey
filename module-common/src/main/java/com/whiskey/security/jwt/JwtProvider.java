package com.whiskey.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties jwtProperties;

    public String generateToken(Long id, String role) {
        return createToken(id, role, jwtProperties.getAccessTokenValidity());
    }

    public String generateRefreshToken(Long id, String role) {
        return createToken(id, role, jwtProperties.getRefreshTokenValidity());
    }

    private String createToken(Long id, String role, long validity) {
        Claims claims = Jwts.claims().setSubject(id.toString());
        claims.put("role", role);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + validity);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecretKey())
            .compact();
    }
}
