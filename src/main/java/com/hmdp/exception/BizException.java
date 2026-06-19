package com.hmdp.exception;

import com.hmdp.enums.ErrorCode;

public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(String message) {
        this(ErrorCode.BIZ_ERROR, message);
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

