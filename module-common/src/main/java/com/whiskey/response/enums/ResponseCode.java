package com.whiskey.response.enums;

public enum ResponseCode {
    MEMBER_REGISTERED("회원가입이 완료되었습니다."),
    WHISKEY_FETCHED("위스키 정보를 조회했습니다.");

    private final String message;

    ResponseCode(String message) {
        this.message = message;
    }

    public String getCode() {
        return this.name();
    }

    public String getMessage() {
        return message;
    }
}
