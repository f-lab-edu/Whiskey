package com.whiskey.batch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.whiskey.security.jwt.JwtTokenProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BatchControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("비인증 요청은 배치 엔드포인트에 접근할 수 없다")
    void 비인증_배치요청_거부() throws Exception {
        mockMvc.perform(post("/api/batch/review-dummy-data"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("USER 권한 토큰은 배치 엔드포인트에서 403")
    void USER권한_배치요청_금지() throws Exception {
        String userToken = jwtTokenProvider.generateToken(
            1L, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(post("/api/batch/review-dummy-data")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }
}
