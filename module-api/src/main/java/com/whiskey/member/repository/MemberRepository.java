package com.whiskey.member.repository;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findOneByEmailAndStatusIsNot(String email, MemberStatus status);
}
