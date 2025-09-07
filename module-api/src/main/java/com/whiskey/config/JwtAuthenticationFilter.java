package com.whiskey.config;

import com.whiskey.domain.auth.service.AuthService;
import com.whiskey.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${dev.enabled:false}")
    private boolean jwtBypassEnabled;

    @Value("${dev.header-name:bypass-jwt-auth}")
    private String jwtBypassHeaderName;

    @Value("${dev.token}")
    private String jwtBypassToken;

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(jwtBypassEnabled) {
            String devToken = request.getHeader(jwtBypassHeaderName);
            if(jwtBypassToken.equals(devToken)) {

                Authentication devAuth = new UsernamePasswordAuthenticationToken("dev-user", null, Arrays.asList(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
                ));

                SecurityContextHolder.getContext().setAuthentication(devAuth);

                filterChain.doFilter(request, response);
                return;
            }
        }

        String token = getToken(request);

        if(token != null && authService.isValidAccessToken(token)) {
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
