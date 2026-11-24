package com.shop.common.exception;

public class ConflictException extends BusinessException {

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
