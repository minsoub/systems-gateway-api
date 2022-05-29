package com.bithumbsystems.exception;

import com.bithumbsystems.model.enums.ErrorCode;

public class GatewayException extends RuntimeException {
    public GatewayException(ErrorCode errorCode) {
        super(String.valueOf(errorCode));
    }
}
