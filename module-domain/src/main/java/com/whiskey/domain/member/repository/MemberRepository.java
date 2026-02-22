package com.whiskey.domain.member.repository;

import com.whiskey.domain.member.Member;
import com.whiskey.domain.member.enums.MemberStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmailAndStatus(String email, MemberStatus memberStatus);

    Optional<Member> findByEmailAndStatus(String email, MemberStatus memberStatus);

    Optional<Member> findByEmail(String email);
}
