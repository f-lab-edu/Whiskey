package com.whiskey.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberStatus;
import com.whiskey.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    void findByEmailAndStatus() {
        Member member = Member.builder()
            .email("tester11@example.com")
            .passwordHash("tester11@@")
            .status(MemberStatus.ACTIVE)
            .memberName("테스트")
            .build();

        memberRepository.save(member);

        var found = memberRepository.findByEmailAndStatus("tester11@example.com", MemberStatus.ACTIVE);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("tester11@example.com");
    }
}