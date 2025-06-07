package com.whiskey.auth.service;

import com.whiskey.domain.login.LoginToken;
import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberRole;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.exception.CommonErrorCode;
import com.whiskey.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final LoginService loginService;

    public LoginToken login(String email, String password) {
        Member member = checkMemberInfo(email, password);
        return loginService.login(member.getId(), MemberRole.USER);
    }

    private Member checkMemberInfo(String email, String password) {
        Member member = memberRepository.findOneByEmailAndStatusIsNot(email, MemberStatus.WITHDRAW)
            .orElseThrow(() -> CommonErrorCode.NOT_FOUND.exception("입력하신 정보와 일치하는 계정이 없습니다."));

        checkMemberStatus(member.getStatus());

        if (!passwordEncoder.matches(password, member.getPasswordHash())) {
            throw CommonErrorCode.NOT_FOUND.exception("일치하는 로그인 정보가 없습니다.");
        }
        return member;
    }

    private void checkMemberStatus(MemberStatus isStatus) {
        if (isStatus == MemberStatus.INACTIVE) {
            throw CommonErrorCode.NOT_FOUND.exception("휴면 계정입니다. 휴면 해제를 해주세요.");
        }
    }
}
