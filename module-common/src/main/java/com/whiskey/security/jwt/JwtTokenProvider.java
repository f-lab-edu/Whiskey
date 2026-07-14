package com.whiskey.security.jwt;

import com.whiskey.exception.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecretKey());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long id, Collection<? extends GrantedAuthority> authorities) {
        return createToken(id, authorities, jwtProperties.getAccessTokenValidity());
    }

    public String generateRefreshToken(Long id) {
        return createToken(id, Collections.emptyList(), jwtProperties.getRefreshTokenValidity());
    }

    private String createToken(Long id, Collection<? extends GrantedAuthority> authorities, long validityTime) {
        Claims claims = Jwts.claims().setSubject(id.toString());

        if (!authorities.isEmpty()) {
            claims.put("roles", authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityTime);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();

        Collection<? extends GrantedAuthority> authorities = getAuthorities(claims);

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Claims claims) {
        List<String> roles = (List<String>) claims.get("roles");

        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }

        return roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims parseClaims(String refreshToken) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();
        }
        catch (ExpiredJwtException e) {
            return e.getClaims();
        }
        catch(JwtException e) {
            throw AuthErrorCode.UNAUTHENTICATED.exception("유효하지 않은 JWT입니다.");
        }
    }

    public Long getAccessTokenValidityTime() {
        return jwtProperties.getAccessTokenValidity();
    }

    public Long getRefreshTokenValidityTime() {
        return jwtProperties.getRefreshTokenValidity();
    }

    public Long getMemberIdFromRefreshToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken);
        return Long.parseLong(claims.getSubject());
    }
}
