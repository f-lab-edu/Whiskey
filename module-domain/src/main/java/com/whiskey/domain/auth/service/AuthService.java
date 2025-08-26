package com.whiskey.domain.auth.service;

import com.whiskey.domain.auth.JwtResponse;
import com.whiskey.domain.auth.MemberInfo;
import com.whiskey.domain.auth.RefreshToken;
import com.whiskey.domain.auth.dto.TokenInfo;
import com.whiskey.domain.auth.repository.RefreshTokenRepository;
import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.domain.member.repository.MemberRepository;
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
        Member member = memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE)
            .orElseThrow(() -> ErrorCode.UNAUTHORIZED.exception("존재하지 않는 회원입니다."));

        if(!passwordEncoder.matches(password, member.getPasswordHash())) {
            throw ErrorCode.UNAUTHORIZED.exception("비밀번호가 일치하지 않습니다.");
        }

        if(member.getStatus() != MemberStatus.ACTIVE) {
            throw ErrorCode.UNAUTHORIZED.exception("비활성화된 계정입니다.");
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
            throw ErrorCode.UNAUTHORIZED.exception("Refresh token이 유효하지 않습니다.");
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
