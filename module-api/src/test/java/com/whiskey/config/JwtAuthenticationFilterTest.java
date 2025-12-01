package com.whiskey.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.whiskey.domain.auth.service.AuthService;
import com.whiskey.security.jwt.JwtTokenProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthService authService;

    @Value("${dev.header-name:bypass-jwt-auth}")
    private String jwtBypassHeaderName;

    @Value("${dev.token}")
    private String jwtBypassToken;

    @Test
    @DisplayName("유효한 JWT 토큰으로 인증 성공")
    void 유효한_JWT토큰_인증성공() throws Exception {
        String validToken = "valid.jwt.token";
        Authentication mockAuthentication = new UsernamePasswordAuthenticationToken("1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(authService.isValidAccessToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(validToken)).thenReturn(mockAuthentication);

        // when & then
        mockMvc.perform(get("/api/test")
                .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk());

        verify(authService).isValidAccessToken(validToken);
        verify(jwtTokenProvider).getAuthentication(validToken);
    }

    @Test
    @DisplayName("개발 환경 JWT 우회 기능 동작 확인")
    void JWT_우회_정상동작() throws Exception {
        mockMvc.perform(get("/api/test")
                .header(jwtBypassHeaderName, jwtBypassToken))
            .andExpect(status().isOk());

        // JWT 관련 서비스 호출되지 않음을 확인
        verify(authService, never()).isValidAccessToken(any());
        verify(jwtTokenProvider, never()).getAuthentication(any());
    }

    @Test
    @DisplayName("JWT 우회 헤더가 틀렸을 때 인증 실패")
    void JWT_우회_인증실패() throws Exception {
        mockMvc.perform(get("/api/test")
                .header(jwtBypassHeaderName, "wrong-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 인증 실패")
    void 유효하지_않은_JWT토큰_인증실패() throws Exception {
        String invalidToken = "invalid.jwt.token";
        when(authService.isValidAccessToken(invalidToken)).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/test")
                .header("Authorization", "Bearer " + invalidToken))
            .andExpect(status().isUnauthorized());

        verify(jwtTokenProvider, never()).getAuthentication(any());
    }

    @Test
    @DisplayName("JWT 토큰이 없을때 API 호출")
    void JWT_토큰없이_API_호출() throws Exception {
        mockMvc.perform(get("/api/test"))
            .andExpect(status().isUnauthorized());
    }
}