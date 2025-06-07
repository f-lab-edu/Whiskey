package com.whiskey.member.service;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.member.dto.MemberRegisterValue;
import com.whiskey.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(MemberRegisterValue memberDto) {
        // 회원가입 약관동의 체크 확인

        // 중복가입 체크

        String encryptPassword = passwordEncoder.encode(memberDto.password());
        Member member = Member.builder()
            .passwordHash(encryptPassword)
            .memberName(memberDto.memberName())
            .email(memberDto.email())
            .status(MemberStatus.ACTIVE)
            .build();

        memberRepository.save(member);
        
        // H2 활용하여 DB 저장확인
    }
}
