package com.whiskey.domain.member.service;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.dto.MemberInfo;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.dto.ValidationErrorValue;
import com.whiskey.exception.ErrorCode;
import com.whiskey.domain.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(String email, String password, String memberName) {
        // 회원가입 약관동의 체크 확인

        // 중복가입 체크
        checkDuplicate(email);

        try {
            String encryptPassword = passwordEncoder.encode(password);
            Member member = Member.builder()
                .passwordHash(encryptPassword)
                .memberName(memberName)
                .email(email)
                .status(MemberStatus.ACTIVE)
                .build();

            memberRepository.save(member);
        }
        catch(DataIntegrityViolationException e) {
            throw ErrorCode.CONFLICT.exception("이미 가입된 이메일입니다.");
        }
    }

    private void checkDuplicate(String email) {
        if(memberRepository.existsByEmailAndStatus(email, MemberStatus.ACTIVE)) {
            ValidationErrorValue errorValue = new ValidationErrorValue("email", email, "duplicate");
            throw ErrorCode.CONFLICT.exception("이미 가입된 이메일입니다.", errorValue);
        }
    }

    public MemberInfo getMemberById(Long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        return new MemberInfo(
            member.getId(),
            member.getMemberName(),
            member.getEmail()
        );
    }

    public Member checkExistMember(long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> ErrorCode.NOT_FOUND.exception("존재하지 않는 회원입니다."));
    }
}
