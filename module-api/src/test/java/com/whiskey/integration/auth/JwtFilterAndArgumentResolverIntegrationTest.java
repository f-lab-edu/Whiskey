package com.whiskey.integration.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Value;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"dev.enabled=true"})
public class JwtFilterAndArgumentResolverIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${dev.header-name:bypass-jwt-auth}")
    private String jwtBypassHeaderName;

    @Value("${dev.token}")
    private String jwtBypassToken;

    @Test
    @DisplayName("JWT 우회 모드에서 ArgumentResolver까지 정상 동작")
    void JWT우회모드_전체플로우_정상동작() throws Exception {
        // when & then
        mockMvc.perform(get("/api/test/member-id-check")
                .header(jwtBypassHeaderName, jwtBypassToken))
            .andExpect(status().isOk())
            .andExpect(content().string("3"))
            .andDo(print());
    }

    @Test
    @DisplayName("JWT 우회 토큰이 틀렸을 때 ArgumentResolver에서 인증 실패")
    void JWT우회토큰틀림_ArgumentResolver에서_인증실패() throws Exception {
        // when & then
        mockMvc.perform(get("/api/test/member-id-check")
                .header(jwtBypassHeaderName, "wrong-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("헤더 없이 요청시 ArgumentResolver에서 인증 실패")
    void 헤더없이요청_ArgumentResolver에서_인증실패() throws Exception {
        // when & then
        mockMvc.perform(get("/api/test/member-id-check"))
            .andExpect(status().isUnauthorized());
    }
}
