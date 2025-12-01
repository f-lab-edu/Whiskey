package com.whiskey.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        memberRepository.save(
            Member.builder()
                .email("tester22@example.com")
                .passwordHash(passwordEncoder.encode("tester22@@"))
                .status(MemberStatus.ACTIVE)
                .memberName("테스터22")
                .build()
        );
    }

    @Test
    void login_success_return_jwt_token() throws Exception {
        String json = "{\"email\":\"tester22@example.com\", \"password\":\"tester22@@\"}";

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.memberInfo.email").value("tester22@example.com"));
    }
}
