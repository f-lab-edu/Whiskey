package com.whiskey.config;

import com.whiskey.domain.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                    .requestMatchers("/api/test/**", "/api/batch/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/request-payment.html", "/api/payments/payment-success", "/api/payments/payment-fail").permitAll()

                    // Member
                    .requestMatchers(HttpMethod.POST, "/api/members").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/members/{id}").authenticated()

                    // Authentication
                    .requestMatchers(HttpMethod.POST, "/api/auth/token/refresh", "/api/auth/login").permitAll()

                    // Whiskey (R만 USER, ADMIN 가능, CUD는 ADMIN만)
                    .requestMatchers(HttpMethod.GET, "/api/whiskey/**").authenticated()
                    .requestMatchers("/api/whiskey/**").hasRole(Role.ADMIN.getRole())

                    // Reviews
                    .requestMatchers(HttpMethod.GET, "/api/whiskey/{id}/reviews").authenticated()
                    .requestMatchers("/api/reviews/**").authenticated()

                    // Order
                    .requestMatchers(HttpMethod.POST, "/api/order").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/order/{orderId}/cancel").authenticated()

                    // Payment
                    .requestMatchers(HttpMethod.POST, "/api/payments/**").authenticated()
                ).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
