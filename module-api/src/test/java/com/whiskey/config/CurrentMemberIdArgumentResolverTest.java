package com.whiskey.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.whiskey.annotation.CurrentMemberId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class CurrentMemberIdArgumentResolverTest {

    @InjectMocks
    private CurrentMemberIdArgumentResolver argumentResolver;

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private ModelAndViewContainer mavContainer;

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private WebDataBinderFactory binderFactory;

    @Test
    @DisplayName("정상적인 회원ID로 ArgumentResolver 동작 확인")
    void 정상적인_회원ID_ArgumentResolver_동작확인() throws Exception {
        Authentication testAuth = new UsernamePasswordAuthenticationToken("3", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(testAuth);

        Object result = argumentResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("JWT 우회 모드에서 ArgumentResolver 동작 확인")
    void JWT_우회모드_ArgumentResolver_동작확인() throws Exception {
        Authentication testAuth = new UsernamePasswordAuthenticationToken("3", null, List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
            )
        );

        SecurityContextHolder.getContext().setAuthentication(testAuth);

        Object result = argumentResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("인증 정보가 없으면 예외 발생 확인")
    void 인증정보_없을때_예외발생확인() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThrows(Exception.class, () -> argumentResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory));
    }

    @Test
    @DisplayName("잘못된 형식의 사용자 ID 입력시 예외 발생 확인")
    void 잘못된_형식의_사용자ID_예외발생_확인() throws Exception {
        // given - 이전 버그 상황 재현
        Authentication invalidAuth = new UsernamePasswordAuthenticationToken("dev-user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(invalidAuth);

        assertThrows(NumberFormatException.class, () -> argumentResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory));
    }

    @Test
    @DisplayName("@CurrentMemberId 어노테이션과 파라미터 Long타입 확인")
    void 어노테이션_파라미터_타입확인() throws Exception {
        when(methodParameter.hasParameterAnnotation(CurrentMemberId.class)).thenReturn(true);
        when(methodParameter.getParameterType()).thenReturn((Class) Long.class);

        boolean result = argumentResolver.supportsParameter(methodParameter);
        assertThat(result).isTrue();
    }
}