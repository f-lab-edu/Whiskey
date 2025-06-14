package com.whiskey.member.service;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.exception.CommonErrorCode;
import com.whiskey.member.dto.MemberRegisterValue;
import com.whiskey.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
        checkDuplicate(memberDto.email(), memberDto.memberName());

        try {
            String encryptPassword = passwordEncoder.encode(memberDto.password());
            Member member = Member.builder()
                .passwordHash(encryptPassword)
                .memberName(memberDto.memberName())
                .email(memberDto.email())
                .status(MemberStatus.ACTIVE)
                .build();

            memberRepository.save(member);
        }
        catch(DataIntegrityViolationException e) {
            throw CommonErrorCode.CONFLICT.exception("이미 가입된 이메일입니다.");
        }
    }

    private void checkDuplicate(String email, String memberName) {
        if(memberRepository.existsByEmailAndStatus(email, MemberStatus.ACTIVE)) {
            Map<String, Object> inputData = Map.of("memberName", memberName, "email", email);
            throw CommonErrorCode.CONFLICT.exception("이미 가입된 이메일입니다.", inputData);
        }
    }
}
