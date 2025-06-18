package com.whiskey.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberRegisterValue(
    @NotBlank @Email @Schema(description = "이메일 : ID로 사용", example = "test@example.com")
    String email,
    @NotBlank @Schema(description = "비밀번호")
    String password,
    @NotBlank @Schema(description = "회원이름")
    String memberName) {

}
