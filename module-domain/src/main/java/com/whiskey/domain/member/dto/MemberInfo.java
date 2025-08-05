package com.whiskey.domain.member.dto;

public record MemberInfo(
    Long id,
    String memberName,
    String email
) {}