package com.whiskey.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OrderErrorCode implements BaseErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 주문입니다."),
    INVALID_ORDER_ITEM(HttpStatus.BAD_REQUEST, "재고가 없는 상품이 포함되어 있습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "주문 상태가 올바르지 않습니다."),
    ORDER_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 주문입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    OrderErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
