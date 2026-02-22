package com.whiskey.domain.auth.service;

import com.whiskey.domain.auth.JwtResponse;
import com.whiskey.domain.auth.MemberInfo;
import com.whiskey.domain.auth.RefreshToken;
import com.whiskey.domain.auth.dto.TokenInfo;
import com.whiskey.domain.auth.repository.RefreshTokenRepository;
import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.domain.member.repository.MemberRepository;
import com.whiskey.exception.AuthErrorCode;
import com.whiskey.exception.BusinessException;
import com.whiskey.exception.ErrorCode;
import com.whiskey.security.jwt.JwtTokenProvider;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;

    public JwtResponse login(String email, String password) {
        try {
            Member member = authenticateMember(email, password);

            tokenService.invalidate(member.getId());

            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
            );

            String accessToken = jwtTokenProvider.generateToken(member.getId(), authorities);
            String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

            tokenService.registerActiveToken(member.getId(), accessToken);

            Long expireTime = jwtTokenProvider.getAccessTokenValidityTime();
            MemberInfo memberInfo = MemberInfo.from(member);

            checkRefreshToken(member.getId(), refreshToken, jwtTokenProvider.getRefreshTokenValidityTime());
            return new JwtResponse(accessToken, refreshToken, "Bearer", expireTime, memberInfo);
        }
        catch(Exception e) {
            throw ErrorCode.UNAUTHORIZED.exception("인증에 실패했습니다.");
        }
    }

    private Member authenticateMember(String email, String password) {
        // 1. 회원 존재 여부 확인
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));

        // 2. 탈퇴 계정 확인
        if(member.isExpired()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_WITHDRAW);
        }

        // 3. 계정 상태 확인
        if(member.isLocked()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_INACTIVE);
        }

        // 4. 비밀번호 확인
        if(!passwordEncoder.matches(password, member.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return member;
    }

    public void checkRefreshToken(Long memberId, String refreshToken, Long expireTime) {
        LocalDateTime expiry = LocalDateTime.now().plus(Duration.ofMillis(expireTime));

        refreshTokenRepository.findByMemberId(memberId)
            .ifPresentOrElse(
                existing -> existing.updateToken(refreshToken, expiry),
                () -> refreshTokenRepository.save(new RefreshToken(memberId, refreshToken, expiry)
            )
        );
    }

    public boolean isValidRefreshToken(Long memberId, String token) {
        Optional<RefreshToken> returnToken = refreshTokenRepository.findByMemberId(memberId);

        if(returnToken.isPresent()) {
            RefreshToken refreshToken = returnToken.get();

            boolean isMatches = refreshToken.getRefreshToken().equals(token);
            boolean notExpired = refreshToken.getExpiryAt().isAfter(LocalDateTime.now());

            return isMatches && notExpired;
        }

        return false;
    }

    public boolean isValidAccessToken(String token) {
        return jwtTokenProvider.validateToken(token) && !tokenService.isBlackListed(token);
    }

    public TokenInfo refreshAccessToken(String refreshToken) {
        Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);

        boolean isValid = isValidRefreshToken(memberId, refreshToken);
        if(!isValid) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER")
        );

        String newAccessToken = jwtTokenProvider.generateToken(memberId, authorities);

        return new TokenInfo(
            newAccessToken,
            refreshToken,
            "Bearer",
            jwtTokenProvider.getAccessTokenValidityTime()
        );
    }
}
