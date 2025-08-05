package com.whiskey.member.dto;

import com.whiskey.domain.member.dto.MemberInfo;

public record MemberResponse(
    Long id,
    String memberName,
    String email
) {
    public static MemberResponse from(MemberInfo memberInfo) {
        return new MemberResponse(
            memberInfo.id(),
            memberInfo.memberName(),
            memberInfo.email()
        );
    }
}