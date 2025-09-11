package com.whiskey.config;

import com.whiskey.domain.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                authorize -> authorize
                    .requestMatchers("/swagger-ui/**", "/swagger-resources/**", "/v3/api-docs/**", "/v3/api-docs").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/token/refresh").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/members/{id}").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/whiskey").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers(HttpMethod.PUT,"/api/whiskey/{id}").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers(HttpMethod.DELETE, "/api/whiskey/{id}").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers(HttpMethod.GET, "/api/whiskey/{id}").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers(HttpMethod.GET, "/api/whiskey").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers(HttpMethod.POST, "/api/review").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers(HttpMethod.PUT, "/api/review/{id}").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers(HttpMethod.GET, "/api/whiskey/{id}/reviews").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers(HttpMethod.GET, "/api/test").hasAnyRole(Role.ADMIN.getRole(), Role.USER.getRole())
                    .requestMatchers("/api/test/**").permitAll()
                ).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
